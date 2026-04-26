# Convenções

## Regras

- Controllers enxutos — lógica de negócio somente no Service
- Repositories apenas persistência
- Injeção de dependência via construtor
- DTOs para entrada/saída; Mapper para conversão; Enum para valores fixos
- Evitar valores nulos; usar nomes descritivos

---

## Nomenclatura

| Classe | Padrão | Exemplo |
|--------|--------|---------|
| Controller | `<Resource>Controller` | `UsersController` |
| Service | `<Resource>Service` | `UsersService` |
| Repository | `<Resource>Repository` | `UsersRepository` |
| Entity | `<Resource>Entity` | `User` |
| Mapper | `<Resource>Mapper` | `UsersMapper` |
| EventPublisher | `<Resource>EventPublisher` | `UsersEventPublisher` |
| Request | `<Resource><Action>Request` | `UsersCreateRequest`, `UsersUpdateRequest` |
| Response | `<Resource><Action>Response` | `UsersCreatedResponse`, `UsersResponse` |
