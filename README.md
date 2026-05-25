# user-api

API REST de gestão de usuários e famílias, orientada a eventos.

## Stack

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.6 |
| SQL Server | Compartilhado com Keycloak |
| Flyway | Migrações SQL Server |
| ActiveMQ | Eventos assíncronos |
| Keycloak | Servidor de identidade (OAuth2/JWT) |

## Pré-requisitos

Os serviços abaixo devem estar rodando **antes** de iniciar a aplicação:

- **SQL Server** — `localhost:1433` (banco `keycloak`)
- **ActiveMQ** — `tcp://localhost:61616`
- **Keycloak** — `http://localhost:9999` (realm `development`, client `dev-client`)

## Arquitetura

```
src/main/java/br/com/user/
├── config/          # Segurança, Keycloak, tratamento de erros, ActiveMQ
└── modules/
    ├── user/        # CRUD de usuário (Keycloak REST API + JDBC direto nas tabelas do Keycloak)
    ├── family/      # Família e membros (JPA + Spring Data)
    └── event/       # Publicação de eventos na fila ActiveMQ "events"
```

**Dados de usuário** residem nas tabelas do Keycloak (`USER_ENTITY`, `REALM`) — não há entidade JPA própria para usuário. Operações de leitura usam `KeycloakUserDao` (JDBC); operações de escrita usam `KeycloakAdminClient` (REST).

**Eventos** são publicados após toda mutação (create/update/delete) na fila `events` do ActiveMQ.

## Banco de dados

Migrações gerenciadas pelo Flyway em `src/main/resources/db/migration/` (`V{n}__descricao.sql`).

Tabelas próprias: `FAMILY_ENTITY`, `FAMILY_MEMBER`.  
Tabelas do Keycloak (somente leitura via JDBC): `USER_ENTITY`, `REALM`.

## Autenticação

Todos os endpoints protegidos exigem um **Bearer Token JWT** obtido via Keycloak (`dev-client`).

```
Authorization: Bearer <token>
```

Roles aceitas: `USER`, `ADMIN` (extraídas de `realm_access.roles` no JWT).

Endpoints públicos: `/actuator/health`, `/actuator/info`, `/api/v1/public/**`.

---
## Documentação da API

A especificação OpenAPI completa está em [docs/openapi/spec.html](docs/openapi/spec.html).
