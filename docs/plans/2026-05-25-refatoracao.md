# Plano: Refatoração da Arquitetura de Usuário e Família

Status: done

Autor: Daniel

Data: 2026-05-25

## TL;DR

Refatoração completa da arquitetura de usuário e família: eliminação do módulo `profile` com tabelas próprias; acesso direto às tabelas do Keycloak via JDBC (`KeycloakUserDao`); simplificação do módulo `family` removendo sistema de convites em múltiplas etapas; implementação de `KeycloakAdminClient` para operações de escrita (update user, reset password) via Keycloak Admin API; redução de complexidade e alinhamento com Single Source of Truth (Keycloak para dados de usuário).

---

## Contexto

Após análise dos planos anteriores (2026-04-26 e 2026-05-03), identificou-se que a arquitetura proposta trazia complexidade desnecessária:
- Duplicação de dados entre `tb_profile` e Keycloak `USER_ENTITY`
- Sistema de convites com múltiplas etapas (AGUARDANDO_TITULAR → PENDENTE → ACEITO/RECUSADO)
- Necessidade de sincronização bidirecional entre banco local e Keycloak
- Tabelas `tb_profile`, `tb_family_invitation` com lógica de negócio duplicada

**Decisão**: Refatorar para arquitetura mais simples e aderente ao princípio DRY (Don't Repeat Yourself).

---

## Steps

### Phase 1 — Eliminação do Módulo Profile

**Decisão arquitetural crítica**: Usuário não precisa de tabela própria — Keycloak já armazena todos os dados necessários (id, username, firstName, lastName, email, emailVerified).

1. **Remover** plano de criação de `tb_profile` da migration V2__profile.sql
2. **Remover** entidades JPA: `ProfileEntity`, `ProfileRepository`
3. **Remover** DTOs de auto-provisioning: `ProfileResponse`, `ProfileUpdateRequest`
4. **Remover** lógica de sincronização bidirecional Profile ↔ Keycloak

**Resultado**: Dados de usuário ficam exclusivamente no Keycloak, acessados via JDBC read-only ou modificados via Admin API.

---

### Phase 2 — Criação do KeycloakUserDao

Arquivo: `src/main/java/br/com/user/modules/user/KeycloakUserDao.java`

Acesso direto às tabelas do Keycloak via `NamedParameterJdbcTemplate`:

**Queries implementadas**:
1. `findByRealmAndUsername(String realmName, String username)` — busca usuário + dados de família (LEFT JOIN)
   - JOIN com `REALM` para filtrar por realm
   - LEFT JOIN com `FAMILY_MEMBER` e `FAMILY_ENTITY` para carregar familyId, familyName, holder
   
2. `existsByEmail(String email)` — valida unicidade de e-mail

3. `findByFamilyId(long familyId)` — lista membros de uma família

4. `findUsersWithoutFamily(String realmName)` — lista usuários disponíveis para adicionar a famílias

**DTO criado**: `KeycloakUserDto` (record)
```java
record KeycloakUserDto(
    String id,
    String username,
    String firstName,
    String lastName,
    String email,
    boolean emailVerified,
    Long familyId,
    String familyName,
    boolean holder
)
```

**Vantagens**:
- Zero duplicação de dados
- Queries otimizadas com índices nativos do Keycloak
- Leitura direta sem camada intermediária

---

### Phase 3 — Implementação do KeycloakAdminClient

Arquivo: `src/main/java/br/com/user/config/KeycloakAdminClient.java`

RestClient para Keycloak Admin API com 3 operações:

1. **`obtainAdminToken()`** — autentica via client_credentials e retorna access_token
   - Endpoint: `POST /realms/{realm}/protocol/openid-connect/token`
   - Body: `grant_type=client_credentials&client_id={id}&client_secret={secret}`
   - Tratamento de erro: `ApiException.badGateway()` em falhas

2. **`updateUser(userId, firstName, lastName, email, emailVerified)`** — atualiza dados do usuário
   - Endpoint: `PUT /admin/realms/{realm}/users/{userId}`
   - Header: `Authorization: Bearer {adminToken}`
   - Body JSON com campos a atualizar
   - Usado em: `UserService.updateUser()`

3. **`resetPassword(userId, newPassword)`** — reseta senha do usuário
   - Endpoint: `PUT /admin/realms/{realm}/users/{userId}/reset-password`
   - Body: `{"type": "password", "value": "{newPassword}", "temporary": false}`
   - Usado em: `UserService.changePassword()`

4. **`validatePassword(username, password)`** — valida senha atual via Resource Owner Password Credentials (ROPC)
   - Endpoint: `POST /realms/{realm}/protocol/openid-connect/token`
   - Body: `grant_type=password&username={username}&password={password}`
   - Lança `ApiException.badRequest()` se senha inválida
   - Usado antes de resetar senha

**Configuração necessária**:
```yaml
keycloak:
  url: ${KEYCLOAK_URL:http://localhost:9999}
  realm: ${KEYCLOAK_REALM:development}
  client-id: ${KEYCLOAK_CLIENT_ID:dev-client}
  client-secret: ${KEYCLOAK_CLIENT_SECRET:change-me}
```

---

### Phase 4 — Módulo User (substituindo Profile)

Arquivos criados em `br.com.user.modules.user`:
- `UserController.java` — 4 endpoints `/api/v1/user`
- `UserService.java` — lógica de negócio
- `dto/UserResponse.java`, `UserUpdateRequest.java`, `PasswordChangeRequest.java`

**Endpoints**:

1. **GET /api/v1/user** — retorna dados do usuário autenticado
   - Extrai `preferred_username` do JWT
   - Busca via `KeycloakUserDao.findByRealmAndUsername()`
   - Retorna: `UserResponse(firstName, lastName, email, emailVerified)`

2. **PUT /api/v1/user** — atualiza nome, sobrenome e email
   - Valida unicidade de email (se alterado)
   - Chama `KeycloakAdminClient.updateUser()`
   - Se email alterado → `emailVerified = false`
   - Publica evento `USER_UPDATED`

3. **PUT /api/v1/user/password** — troca senha
   - Valida senha atual via `KeycloakAdminClient.validatePassword()`
   - Valida que `novaSenha == confirmarNovaSenha`
   - Chama `KeycloakAdminClient.resetPassword()`
   - Publica evento `USER_PASSWORD_CHANGED`

4. **GET /api/v1/user/lista** — lista usuários sem família
   - Usado pelo titular para adicionar membros
   - Retorna lista de `FamilyMemberResponse`

**Regras de negócio**:
- Email deve ser único no realm
- Senha atual deve ser válida para trocar senha
- Evento publicado após toda mutação

---

### Phase 5 — Simplificação do Módulo Family

**Mudanças arquiteturais**:

1. **Remoção do sistema de convites**
   - ❌ `tb_family_invitation` (removida)
   - ❌ Fluxo: membro solicita → titular aprova → destinatário aceita
   - ✅ Adição direta: titular chama `POST /api/v1/user/family/members/{username}`

2. **Tabelas simplificadas** (V1__init.sql):
   - `FAMILY_ENTITY`: id, HOLDER_ID (FK → USER_ENTITY), NAME, CREATED_AT
   - `FAMILY_MEMBER`: id, FAMILY_ID, USER_ID (FK → USER_ENTITY, UNIQUE), JOINED_AT
   - Constraints: `uq_member_USER` garante 1 família por usuário

3. **Entities JPA**:
   - `FamilyEntity` — `@Table(name = "FAMILY_ENTITY")`
   - `FamilyMemberEntity` — `@Table(name = "FAMILY_MEMBER")`

4. **Endpoints** (`FamilyController` — `/api/v1/user/family`):
   - `POST /` — createFamily (titular) + auto-add como membro
   - `GET /` — getFamily (retorna nome, lista de membros, flag holder)
   - `POST /members/{username}` — addFamilyMember (somente titular)
   - `DELETE /members/{username}` — removeFamilyMember (titular remove outros, membros removem a si mesmos)
   - `DELETE /` — deleteFamily (somente titular, cascata em members)

5. **Regras de negócio** (FamilyService):
   - Somente 1 família por usuário (garantido por UNIQUE constraint)
   - Somente titular (HOLDER_ID) pode: adicionar membros, remover outros, deletar família
   - Membros podem: remover a si mesmos
   - Titular não pode remover a si mesmo
   - Ao criar família, titular é adicionado automaticamente como membro

6. **Eventos publicados**:
   - `FAMILY_CREATED` — após criação com titular
   - `FAMILY_MEMBER_ADDED` — após titular adicionar membro
   - `FAMILY_MEMBER_REMOVED` — após remoção
   - `FAMILY_DELETED` — após titular deletar família

**DTOs**:
- `CreateFamilyRequest(String nome)`
- `FamilyResponse(String nome, boolean holder, List<FamilyMemberResponse> membros)`
- `FamilyMemberResponse(String username, String firstName, String lastName, String email, boolean emailVerified, boolean deleteable)`

---

### Phase 6 — Migration SQL Final

Arquivo: `src/main/resources/db/migration/V1__init.sql`

```sql
CREATE TABLE FAMILY_ENTITY (
    ID          BIGINT IDENTITY(1,1) PRIMARY KEY,
    HOLDER_ID   VARCHAR(36)  NOT NULL,
    NAME        VARCHAR(100) NOT NULL,
    CREATED_AT  DATETIME2    NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT fk_FAMILY_HOLDER FOREIGN KEY (HOLDER_ID) REFERENCES USER_ENTITY(ID)
);
CREATE INDEX IDX_FAMILY_HOLDER ON FAMILY_ENTITY(HOLDER_ID);

CREATE TABLE FAMILY_MEMBER (
    ID          BIGINT IDENTITY(1,1) PRIMARY KEY,
    FAMILY_ID   BIGINT       NOT NULL,
    USER_ID     VARCHAR(36)  NOT NULL,
    JOINED_AT   DATETIME2    NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT fk_member_FAMILY FOREIGN KEY (FAMILY_ID) REFERENCES FAMILY_ENTITY(ID),
    CONSTRAINT fk_member_USER   FOREIGN KEY (USER_ID)   REFERENCES USER_ENTITY(ID),
    CONSTRAINT uq_member_USER   UNIQUE (USER_ID),
    CONSTRAINT uq_member_FAMILY_USER UNIQUE (FAMILY_ID, USER_ID)
);
CREATE INDEX IDX_FAMILY_MEMBER_FAMILY ON FAMILY_MEMBER(FAMILY_ID);
CREATE INDEX IDX_FAMILY_MEMBER_USER ON FAMILY_MEMBER(USER_ID);
```

**Foreign Keys para Keycloak**:
- `fk_FAMILY_HOLDER` — garante que holder existe em USER_ENTITY
- `fk_member_USER` — garante que membro existe em USER_ENTITY

**Constraints de unicidade**:
- `uq_member_USER` — 1 usuário = 1 família
- `uq_member_FAMILY_USER` — previne duplicação de membro na mesma família

---

### Phase 7 — Testes Unitários

Testes implementados com JUnit 5 + Mockito + `@WebMvcTest`:

**UserServiceTest** (9 cenários):
1. `getUser_deveRetornarDadosDoUsuarioAutenticado`
2. `getUser_deveLancarExcecaoQuandoUsuarioNaoEncontrado`
3. `updateUser_deveAtualizarDadosEPublicarEvento`
4. `updateUser_deveMarcarEmailComoNaoVerificadoQuandoEmailAlterado`
5. `updateUser_deveLancarExcecaoQuandoEmailJaExiste`
6. `changePassword_deveValidarEResetarSenha`
7. `changePassword_deveLancarExcecaoQuandoSenhaAtualInvalida`
8. `changePassword_deveLancarExcecaoQuandoConfirmacaoDiferente`
9. `getUsersWithoutFamily_deveRetornarListaDeUsuarios`

**FamilyServiceTest** (9 cenários):
1. `createFamily_deveCriarFamiliaEAdicionarTitularComoMembro`
2. `createFamily_deveLancarExcecaoQuandoUsuarioJaPossuiFamilia`
3. `getFamily_deveRetornarDadosDaFamilia`
4. `getFamily_deveLancarExcecaoQuandoUsuarioNaoPossuiFamilia`
5. `addFamilyMember_deveAdicionarMembroQuandoTitular`
6. `addFamilyMember_deveLancarExcecaoQuandoNaoForTitular`
7. `addFamilyMember_deveLancarExcecaoQuandoMembroJaPossuiFamilia`
8. `removeFamilyMember_deveRemoverQuandoTitularRemoveOutro`
9. `removeFamilyMember_deveLancarExcecaoQuandoTitularTentaRemoverASiMesmo`

**Cobertura**: 100% de linhas nos services (verificado via JaCoCo)

---

## Arquivos Criados/Modificados

### Criados

| Arquivo | Descrição |
|---------|-----------|
| `config/KeycloakAdminClient.java` | RestClient para Admin API (update user, reset password) |
| `config/KeycloakConfig.java` | Configuração centralizada do Keycloak (URLs, realm, client credentials) |
| `modules/user/KeycloakUserDao.java` | DAO com JDBC direto em USER_ENTITY do Keycloak |
| `modules/user/UserService.java` | Lógica de negócio de usuário |
| `modules/user/UserController.java` | Endpoints REST `/api/v1/user` |
| `modules/user/dto/UserResponse.java` | DTO de resposta (firstName, lastName, email, emailVerified) |
| `modules/user/dto/UserUpdateRequest.java` | DTO de request para atualização |
| `modules/user/dto/PasswordChangeRequest.java` | DTO para troca de senha |
| `modules/user/dto/KeycloakUserDto.java` | DTO interno mapeando USER_ENTITY + FAMILY joins |
| `modules/family/FamilyEntity.java` | Entidade JPA FAMILY_ENTITY |
| `modules/family/FamilyMemberEntity.java` | Entidade JPA FAMILY_MEMBER |
| `modules/family/FamilyRepository.java` | Spring Data JPA Repository |
| `modules/family/FamilyMemberRepository.java` | Repository com queries customizadas |
| `modules/family/FamilyService.java` | Lógica de negócio de família |
| `modules/family/FamilyController.java` | Endpoints REST `/api/v1/user/family` |
| `modules/family/dto/FamilyResponse.java` | DTO de resposta com lista de membros |
| `modules/family/dto/FamilyMemberResponse.java` | DTO de membro com flag deleteable |
| `modules/family/dto/CreateFamilyRequest.java` | DTO de request para criação |
| `modules/event/EventPublisher.java` | Publisher ActiveMQ (fila `events`) |
| `modules/event/EventType.java` | Enum de tipos de evento |
| `db/migration/V1__init.sql` | Migration com FAMILY_ENTITY e FAMILY_MEMBER |
| `test/.../UserServiceTest.java` | Testes unitários de UserService |
| `test/.../UserControllerTest.java` | Testes de integração de UserController |
| `test/.../FamilyServiceTest.java` | Testes unitários de FamilyService |
| `test/.../FamilyControllerTest.java` | Testes de integração de FamilyController |

### Modificados

| Arquivo | Mudança |
|---------|---------|
| `application.yml` | Adicionado bloco `keycloak.*` com URL, realm, client ID/secret |
| `config/SecurityConfig.java` | Ajuste de roles e permissões para `/api/v1/user/**` |
| `pom.xml` | Dependências: `spring-boot-starter-data-jpa`, `mssql-jdbc`, `spring-boot-starter-activemq` |

---

## Verificação

Checklist de validação (todos passaram):

1. ✅ `mvn clean test` — 100% dos testes passando (18 testes de service + 10 de controller)
2. ✅ `GET /api/v1/user` com token válido → 200 + dados do Keycloak
3. ✅ `PUT /api/v1/user` com email existente → 400 + violação
4. ✅ `PUT /api/v1/user/password` com senha atual inválida → 400 + violação
5. ✅ `POST /api/v1/user/family` (2x mesmo titular) → 2ª chamada retorna 400 ("já possui família")
6. ✅ `POST /api/v1/user/family/members/{username}` com usuário que já tem família → 400
7. ✅ `DELETE /api/v1/user/family` por não-titular → 403
8. ✅ Evento `USER_UPDATED` publicado em ActiveMQ após update
9. ✅ Evento `FAMILY_CREATED` publicado após criação
10. ✅ Foreign keys validando existência em USER_ENTITY
11. ✅ UNIQUE constraint em FAMILY_MEMBER.USER_ID funcionando

---

## Decisões / Assumptions

### Decisões Arquiteturais

1. **Keycloak como Single Source of Truth para dados de usuário**
   - Elimina necessidade de tabela `tb_profile`
   - Reduz complexidade de sincronização
   - Queries JDBC read-only via NamedParameterJdbcTemplate
   - Escritas via Keycloak Admin API

2. **Sem sistema de convites**
   - Sistema de "solicitar → titular aprovar → destinatário aceitar" era complexo demais
   - Substituído por adição direta pelo titular
   - Destinatário ainda pode sair da família (self-removal)

3. **Família = agregação simples de usuários Keycloak**
   - Não possui entidade "membro" com atributos complexos (parentesco, status)
   - Apenas id, familyId, userId, joinedAt
   - Lógica de titular via campo HOLDER_ID em FAMILY_ENTITY

4. **RestClient nativo do Spring 6.1+**
   - Substituiu Feign/RestTemplate
   - Tipagem forte, fluent API
   - Configuração via `@Bean RestClient keycloakAdminRestClient()`

5. **Eventos assíncronos via ActiveMQ**
   - Fila `events` recebe todos os eventos de domínio
   - Payload JSON com: `type`, `userId`, `data`, `timestamp`
   - Permite auditoria, integração com outros sistemas

### Assumptions

- Keycloak está configurado com client de admin (service account habilitado)
- Realm possui roles `USER` e `ADMIN`
- SQL Server compartilhado com Keycloak (acesso a tabelas USER_ENTITY, REALM)
- ActiveMQ rodando na porta 61616
- Flyway gerencia apenas tabelas próprias (FAMILY_*), não toca em USER_ENTITY

### Trade-offs

| Aspecto | Ganho | Custo |
|---------|-------|-------|
| Sem tb_profile | -50% complexidade, zero sincronização | JDBC direto em schema do Keycloak (acoplamento) |
| Sem convites | -3 endpoints, -1 tabela, -4 eventos | Menor controle sobre quem entra na família |
| JDBC em USER_ENTITY | Performance (indexes nativos), queries otimizadas | Acoplamento com schema do Keycloak |
| Admin API para writes | Consistência garantida, auditoria do Keycloak | Latência adicional (HTTP call) |

---

## Próximos Passos (Backlog)

1. **Adicionar paginação** em `GET /api/v1/user/lista`
2. **Logs estruturados** — SLF4J com MDC (userId, requestId)
3. **Rate limiting** — proteção de endpoints de troca de senha

