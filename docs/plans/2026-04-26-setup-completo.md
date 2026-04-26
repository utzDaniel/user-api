# Plano: Setup Completo da user-api

Status: done

Autor: Daniel

Data: 2026-04-26

## TL;DR

Gerar `pom.xml` (Java 21, Spring Boot 4.0.5), estrutura Maven, classe `Application`, `application.yml`, Flyway; criar `Dockerfile` + `docker-compose` com `sqlserver`, `activemq` e `keycloak`; configurar Resource Server OAuth2 JWT, Swagger público, ActiveMQ, Actuator e preencher `spec.json`.

---

## Steps

### Phase 1 — Scaffold do projeto

1. Criar `pom.xml` com:
   - Parent: `spring-boot-starter-parent` 4.0.5
   - Dependências: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `mssql-jdbc`, `spring-boot-starter-activemq`, `flyway-core`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-actuator`, `lombok`, `spring-boot-starter-test`
   - `artifactId: user-api`, `groupId: br.com.user`, `version: 0.1.0-SNAPSHOT`

2. Criar estrutura de pacotes:
   - `src/main/java/br/com/user`
   - `src/main/resources`
   - `src/main/resources/db/migration`
   - `src/test/java/br/com/user`

3. Criar artefatos iniciais:
   - `Application.java` (`@SpringBootApplication`, pacote `br.com.user`)
   - `ApplicationTests.java`

### Phase 2 — `application.yml`

4. Configurar `application.yml` com:
   - `spring.application.name: user-api`
   - `spring.datasource` → SQL Server (`${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`)
   - `spring.flyway` → `enabled: true`, `baseline-on-migrate: true`
   - `spring.activemq.broker-url: ${ACTIVEMQ_URL:tcp://localhost:61616}`
   - `spring.security.oauth2.resourceserver.jwt.issuer-uri: ${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM:user-dev}`
   - `springdoc.swagger-ui.path: /swagger-ui/index.html`
   - `springdoc.api-docs.path: /v3/api-docs`
   - `management.endpoints.web.exposure.include: health,info,metrics`
   - `management.endpoint.health.show-details: always`
   - `server.port: 8082`

### Phase 3 — Flyway

5. Criar `src/main/resources/db/migration/V1__init.sql` (idempotente):
   - `SELECT * FROM sys.databases WHERE name = 'user'`

### Phase 4 — Configs (`br.com.user.config`)

6. Criar `SecurityConfig.java`:
   - Form login e HTTP Basic desabilitados
   - OAuth2 Resource Server com validação JWT
   - `JwtAuthenticationConverter` com `setAuthorityPrefix("")`
   - **Rotas públicas**: `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, `/actuator/info`, `/api/v1/public/**`
   - **Autorização por role**:
     - `GET /api/v1/users/**` → autenticado
     - `POST /api/v1/users` → `hasAuthority("ADMIN")`
     - `PUT /api/v1/users/**` → `hasAuthority("ADMIN")`
     - `DELETE /api/v1/users/**` → `hasAuthority("ADMIN")`
     - `GET/PUT /api/v1/users/{id}/roles` → `hasAuthority("ADMIN")`
   - `AuthenticationEntryPoint` customizado → `401 Unauthorized` em JSON
   - `AccessDeniedHandler` customizado → `403 Forbidden` em JSON

7. Criar `OpenApiConfig.java`:
   - Metadados: título `API de Usuários`, versão `1.0.0`, descrição
   - `SecurityScheme` do tipo `bearerAuth` (Bearer JWT)
   - URL do servidor (`http://localhost:8082`)

8. Criar `ActiveMQConfig.java`:
   - `ActiveMQConnectionFactory` configurado com `spring.activemq.broker-url`
   - `JmsTemplate` com `MappingJackson2MessageConverter`
   - `DefaultJmsListenerContainerFactory` para futuros consumers

### Phase 5 — Conteinerização

9. Criar `Dockerfile` (multi-stage):
   - Stage `build`: `maven:3.9-eclipse-temurin-21` — executa `mvn clean package -DskipTests`
   - Stage `runtime`: `eclipse-temurin:21-jre-alpine` — copia o JAR gerado

10. Criar `docker-compose.yml` com serviços:
    - `sqlserver`: `mcr.microsoft.com/mssql/server:2025-latest`, porta `1433`
    - `activemq`: `rmohr/activemq:5.15.9`, portas `61616` + `8161`
    - `keycloak`: `quay.io/keycloak/keycloak`, porta `8081`, com `--import-realm`
    - `app`: build local, porta `8082`, depende dos três serviços acima

11. Criar `docker/keycloak/realm-export.json`:
    - Realm `user-dev`
    - Client `user-client`
    - Roles de realm: `USER`, `ADMIN`
    - Mapper de protocolo para incluir `realm_access.roles` no JWT
    - Usuário de teste com role `ADMIN`

### Phase 6 — `spec.json`

12. Criar `docs/openapi/spec.json` com a spec OpenAPI completa:
    - `info`, `servers` (`http://localhost:8082`), `tags`
    - `paths` para `/api/v1/users` (GET list, POST) e `/api/v1/users/{id}` (GET, PUT, DELETE)
    - `paths` para `/api/v1/users/{id}/roles` (GET, PUT)
    - `components.securitySchemes.bearerAuth` (Bearer JWT)
    - `security: [{bearerAuth: []}]` global
    - Schemas: `UsersCreateRequest`, `UsersUpdateRequest`, `UsersCreatedResponse`, `UsersResponse`

13. Gerar `docs/openapi/spec.html` via `scripts/generate-docs.ps1`

### Phase 7 — Scripts

14. Criar `scripts/generate-docs.ps1` (Redoc CDN, idêntico ao `finance-api`)

---

## Relevant files

| Arquivo | Ação |
|---------|------|
| `pom.xml` | criar |
| `src/main/java/br/com/user/Application.java` | criar |
| `src/main/resources/application.yml` | criar |
| `src/main/resources/db/migration/V1__init.sql` | criar |
| `src/test/java/br/com/user/ApplicationTests.java` | criar |
| `src/main/java/br/com/user/config/SecurityConfig.java` | criar |
| `src/main/java/br/com/user/config/OpenApiConfig.java` | criar |
| `src/main/java/br/com/user/config/ActiveMQConfig.java` | criar |
| `Dockerfile` | criar |
| `docker-compose.yml` | criar |
| `docker/keycloak/realm-export.json` | criar |
| `docs/openapi/spec.json` | criar |
| `scripts/generate-docs.ps1` | criar |

---

## Verification

1. `mvn clean package -DskipTests` — build compila sem erros
2. `docker-compose up` → `http://localhost:8082/swagger-ui/index.html` abre sem página de login
3. `GET http://localhost:8082/actuator/health` → `200 {"status":"UP"}`
4. `GET http://localhost:8082/api/v1/users` sem token → `401 JSON` (sem redirect 302)
5. `POST http://localhost:8082/api/v1/users` com token sem role `ADMIN` → `403 JSON`
6. `mvn clean test` → todos os testes passam

---

## Decisions / Assumptions

- `spring-boot-starter-oauth2-resource-server` nativo — sem `keycloak-spring-boot-starter` (deprecated)
- Authorities sem prefixo `SCOPE_`: via `JwtAuthenticationConverter.setAuthorityPrefix("")`
- Roles via `realm_access.roles` do Keycloak
- Actuator público: apenas `/actuator/health` e `/actuator/info`
- Swagger UI totalmente público
- `spec.json` é a fonte de verdade para o contrato da API
- Porta padrão: `8082`