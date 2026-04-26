# API REST

## Versionamento

- Padrão: `/api/v{version}/<resource>` — ex: `/api/v1/users`
- Nova versão (`v2+`) somente em breaking change (remoção de campos, mudança de tipo, renomeação de endpoint)
- Adição de campos ou novos endpoints: não requer nova versão

## Métodos HTTP

| Método | Ação | Exemplo | Status | Evento |
|--------|------|---------|--------|--------|
| POST | Create | /users | 201 Created | CREATED |
| GET | Retrieve | /users/{id} | 200 OK | RETRIEVED |
| GET | List | /users | 200 OK | LISTED |
| PUT | Update | /users/{id} | 200 OK | UPDATED |
| DELETE | Remove | /users/{id} | 204 No Content | DELETED |

## Nomenclatura de Recursos

- Substantivos no plural, letras minúsculas, hífen para camelCase
- Exemplos: `/users`, `/users/{id}/roles`

## Recursos

| Recurso | Caminho | Descrição |
|---------|---------|-----------|
| Usuários | `/api/v1/users` | Gestão de usuários |
| Roles | `/api/v1/users/{id}/roles` | Gestão de roles do usuário |

## Documentação

Todo endpoint DEVE ser documentado no OpenAPI — ver [Swagger](swagger.md)
