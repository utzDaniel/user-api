# Instruções do Copilot - API de Usuários

## Idioma (OBRIGATÓRIO)

- Todas as respostas DEVEM ser em português (pt-BR)
- Termos técnicos podem permanecer em inglês

---

## Projeto

API REST de gestão de usuários, orientada a eventos.

| Item | Detalhe |
|------|---------|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Build | Maven (`mvn clean package -DskipTests`) |
| Testes | `mvn clean test` — sempre usar `clean` |
| Pacote raiz | `br.com.user` |
| Banco de dados | SQL Server — migrações em `src/main/resources/db/migration/` (Flyway) |
| Broker | ActiveMQ (eventos de domínio) |
| Autenticação | OAuth2 / JWT via Keycloak |
| Lombok | Usado em toda a base de código |
| Infra local | `docker-compose.yml` sobe SQL Server, ActiveMQ e Keycloak |

---

## Documentação de referência

Consulte sempre antes de implementar:

- [Arquitetura](docs/architecture.md) — camadas, fluxo, módulos
- [Convenções](docs/conventions.md) — nomenclatura de classes, DTOs, enums
- [API REST](docs/api.md) — versionamento (`/api/v1/`), métodos HTTP, recursos
- [Eventos](docs/events.md) — convenção `RESOURCE_ACTION`, campos obrigatórios
- [Segurança](docs/security.md) — endpoints protegidos, escopos, tratamento de erros
- [Swagger](docs/swagger.md) — documentação OpenAPI obrigatória por endpoint
- [Testes](docs/testing.md) — estrutura, convenção de nomes, cobertura obrigatória

---

## Checklist para cada implementação

Para **toda** nova funcionalidade ou alteração, o Copilot DEVE:

1. **Documentar endpoint** no OpenAPI (resumo, descrição, request/response + exemplos) → [Swagger](docs/swagger.md)
2. **Publicar evento** em toda operação de escrita (POST/PUT/DELETE) → [Eventos](docs/events.md)
3. **Criar testes unitários** na camada de serviço com JUnit 5 + Mockito → [Testes](docs/testing.md)
   - Validar lógica de negócio, erros e publicação de evento
   - Padrão de nome: `<metodo>Should<comportamentoEsperado>`
4. **Garantir que todos os testes passem** — regressões não são permitidas
5. **Atualizar `docs/plans/README.md`** se um plano de execução foi concluído

---

## Fluxo de criação de novo recurso

```
Controller → Service → Repository → DTO (Request/Response)
     ↓            ↓
  OpenAPI      Evento + Teste unitário
```

Pacote exemplo: `br.com.user.modules.user.{controller,service,repository,dto}`
