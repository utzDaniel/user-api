# Eventos

## Convenção de Nomenclatura

Padrão: `<RESOURCE>_<ACTION>` (maiúsculas)

## Eventos de Escrita (obrigatórios)

| Evento | Endpoint |
|--------|----------|
| USER_CREATED | POST /api/v1/users |
| USER_UPDATED | PUT /api/v1/users/{id} |
| USER_DELETED | DELETE /api/v1/users/{id} |
| PROFILE_UPDATED | PUT /api/v1/profile |
| PROFILE_PASSWORD_CHANGED | PUT /api/v1/profile/password |
| FAMILY_CREATED | POST /api/v1/profile/family |
| FAMILY_INVITATION_REQUESTED | POST /api/v1/profile/family/invitations |
| FAMILY_INVITATION_APPROVED_BY_TITULAR | PUT /api/v1/profile/family/invitations/{id}/approve |
| FAMILY_INVITATION_REJECTED_BY_TITULAR | PUT /api/v1/profile/family/invitations/{id}/reject-titular |
| FAMILY_INVITATION_ACCEPTED | PUT /api/v1/profile/family/invitations/{id}/accept |
| FAMILY_INVITATION_REJECTED | PUT /api/v1/profile/family/invitations/{id}/reject |
| FAMILY_MEMBER_REMOVED | DELETE /api/v1/profile/family/members/{id} |
| FAMILY_MEMBER_UPDATED | PUT /api/v1/profile/family/members/{id} |

## Eventos de Leitura (opcionais — observabilidade)

- USER_RETRIEVED → GET /api/v1/users/{id}
- USER_LISTED → GET /api/v1/users

## Regras

- Publicados somente pela camada de Service
- Toda operação de escrita DEVE publicar um evento
- Eventos devem ser imutáveis
- Eventos de leitura não devem impactar desempenho

## Broker

- ActiveMQ — fila única: `events`

## Campos Obrigatórios

| Campo | Descrição |
|-------|-----------|
| id | Identificador do recurso |
| type | Tipo do evento |
| timestamp | UTC (ISO 8601) |
| userId | Usuário autenticado |
| payload | Dados adicionais (opcional) |

## Exemplo

```json
{
  "type": "USER_CREATED",
  "id": 42,
  "timestamp": "2026-01-01T10:00:00.000Z",
  "userId": "keycloak-subject-uuid",
  "payload": { "name": "João Silva", "email": "joao@email.com" }
}
```
