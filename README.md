# API de Usuários

Serviço RESTful para gestão de usuários — criação, atualização, remoção e controle de roles, com publicação de eventos para integração.

## Tecnologias

- Java 21 / Spring Boot 4.0.5 / Maven
- SQL Server + Flyway
- ActiveMQ
- OAuth2 / JWT (Keycloak)
- OpenAPI 3 (SpringDoc)

## Comandos

> Use `Comandos.bat` para acesso rápido pelo menu interativo.

| Opção | Descrição | Comando |
|-------|-----------|---------|
| 1 | Build completo (build + testes + docs) | `Comandos.bat` → 1 |
| 2 | Build sem testes | `mvn clean install -DskipTests` |
| 3 | Rodar testes | `mvn clean test` |
| 4 | Gerar documentação (`spec.html` via Redoc) | `Comandos.bat` → 4 |

## Documentação Técnica

- [Arquitetura](docs/architecture.md)
- [API REST](docs/api.md)
- [Eventos](docs/events.md)
- [Segurança](docs/security.md)
- [Swagger](docs/swagger.md)
- [Testes](docs/testing.md)
- [Convenções](docs/conventions.md)

## Documentação da API

- [Referência OpenAPI (HTML)](docs/openapi/spec.html)
- Swagger UI: `/swagger-ui/index.html` (público)
- OpenAPI JSON: `/v3/api-docs`
- Actuator: `/actuator/health`, `/actuator/info` (públicos)
