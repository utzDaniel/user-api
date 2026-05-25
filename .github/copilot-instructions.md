# Instruções do Copilot - API de Usuários

## Idioma (OBRIGATÓRIO)

- Todas as respostas DEVEM ser em português (pt-BR)
- Termos técnicos podem permanecer em inglês

---

## Stack

- **Java 21** + **Spring Boot 4.0.6**
- **SQL Server** (compartilhado com o Keycloak — tabelas próprias + queries diretas em `USER_ENTITY`/`REALM`)
- **Flyway** para migrações (apenas SQL Server; testes usam H2)
- **ActiveMQ** para eventos assíncronos (fila `events`)
- **Keycloak** como servidor de identidade (OAuth2 JWT / Resource Server)
- **Lombok** nas entidades JPA
- **Validation API** (`@Valid`, `@Validated`) nos controllers

## Comandos principais

```bash
mvn clean package          # build + testes
mvn test                   # só testes
mvn spring-boot:run        # roda local (porta 8081)
docker compose up --build  # sobe apenas a aplicação (ver nota abaixo)
```

> **Atenção:** o `docker-compose.yml` sobe **somente** a aplicação. SQL Server, ActiveMQ e Keycloak precisam estar rodando externamente.

## Arquitetura

```
src/main/java/br/com/user/
├── config/          # SecurityConfig, KeycloakConfig, GlobalExceptionHandler, ActiveMQConfig, ApiException, Violacao, TimestampUtils
└── modules/
    ├── user/        # CRUD de usuário via Keycloak REST API + KeycloakUserDao (JDBC direto nas tabelas do Keycloak)
    ├── family/      # Família/membros com JPA (FamilyEntity, FamilyMemberEntity) + repositórios Spring Data
    └── event/       # EventPublisher → fila ActiveMQ "events"
```

## Convenções de código

### Controllers
- Sempre `@Validated` na classe, `@Valid` no `@RequestBody`
- Recebem `@AuthenticationPrincipal Jwt jwt` para acessar dados do usuário autenticado
- Prefixo de rota: `/api/v1/`

### DTOs
- Requests e responses são **Java Records** (não classes com getters/setters)
- Ficam em subpacote `dto/` dentro do módulo

### Tratamento de erros
- Lançar `ApiException` para erros de negócio (contém `HttpStatus`, mensagem e opcionalmente lista de `Violacao`)
- O `GlobalExceptionHandler` padroniza todas as respostas de erro com campos: `timestamp`, `status`, `error`, `message`/`violacoes`, `path`
- **Nunca** usar `ResponseStatusException` para lógica de negócio

### Eventos
- Após toda mutação (create/update/delete), publicar evento via `EventPublisher.publish(EventType, userId, payload)`
- `EventType` define os tipos disponíveis (`USER_UPDATED`, `USER_PASSWORD_CHANGED`, `FAMILY_CREATED`, `FAMILY_MEMBER_REMOVED`)

### Acesso a dados de usuário
- Dados de usuário vivem no banco do Keycloak — **não há entidade JPA própria para usuário**
- `KeycloakUserDao` faz queries JDBC com `NamedParameterJdbcTemplate` diretamente nas tabelas `USER_ENTITY`, `REALM` do Keycloak
- Operações de escrita (criar/atualizar/deletar usuário) usam `KeycloakAdminClient` (REST API do Keycloak)

### Segurança
- Stateless, OAuth2 Resource Server com JWT decodificado via JWK Set URI do Keycloak
- Roles extraídas de `realm_access.roles` no JWT e prefixadas com `ROLE_` pelo `JwtAuthenticationConverter`
- Endpoints `/api/v1/user/**` requerem role `USER` ou `ADMIN`
- Endpoints `/actuator/health`, `/actuator/info`, `/api/v1/public/**` são públicos

## Testes

- Framework: JUnit 5 + Mockito + Spring MockMvc (`@WebMvcTest`)
- Banco em testes: H2 (configurado em `src/test/resources/application.yml`)
- Segurança em testes: `spring-security-test` (`@WithMockUser`, `SecurityMockMvcRequestPostProcessors`)
- Nomes de teste em português com `@DisplayName`
- Testes unitários de config ficam em `src/test/java/br/com/user/config/`

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=keycloak;...` | URL do SQL Server |
| `DB_USER` | `sa` | Usuário do banco |
| `DB_PASSWORD` | `YourStrong!Passw0rd` | Senha do banco |
| `ACTIVEMQ_URL` | `tcp://localhost:61616` | Broker ActiveMQ |
| `KEYCLOAK_URL` | `http://localhost:9999` | URL base do Keycloak |
| `KEYCLOAK_REALM` | `development` | Realm do Keycloak |
| `KEYCLOAK_CLIENT_ID` | `dev-client` | Client ID para admin |
| `KEYCLOAK_CLIENT_SECRET` | `change-me` | Client secret para admin |

## Migrações de banco

- Ficam em `src/main/resources/db/migration/` com prefixo `V{n}__descricao.sql`
- Tabelas próprias: `FAMILY_ENTITY`, `FAMILY_MEMBER`
- Tabelas do Keycloak usadas via JDBC (não gerenciadas pelo Flyway): `USER_ENTITY`, `REALM`

