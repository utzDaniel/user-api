# Segurança

## Modelo

Resource Server OAuth2 — valida JWT emitido pelo Keycloak. Não autentica usuários diretamente.

Fluxo: Cliente → Keycloak (obtém JWT) → gateway-api (valida e roteia) → user-api (valida JWT e processa)

## Roles

| Role | Descrição |
|------|-----------|
| `USER` | Acesso básico — leitura do próprio perfil |
| `ADMIN` | Gestão completa de usuários |

## Endpoints

| Endpoint | Acesso |
|----------|--------|
| /api/v1/users/** | ROLE_ADMIN |
| /api/v1/profile/** | ROLE_USER ou ROLE_ADMIN |
| /api/v1/public/** | Público |
| /swagger-ui/** | Público |
| /actuator/health | Público |
| /actuator/info | Público |

## Migração de Segurança

A partir da v2 (2026-05-03) a autorização é baseada em **roles** (`realm_access.roles` do JWT), não mais em scopes.
O converter custom lê `realm_access.roles` e adiciona o prefixo `ROLE_` para integração com Spring Security `hasRole()`.

## Erros de Autenticação

| Cenário | Status |
|---------|--------|
| Token ausente ou inválido | 401 |
| Token expirado | 401 |
| Acesso negado (role insuficiente) | 403 |

## Validação do Token

O token DEVE conter: `iss`, `sub`, `exp`, `iat`, `scope`, `realm_access.roles`  
A API valida: assinatura, expiração, emissor e audience (se configurado)

## Boas Práticas

- Nunca exponha senhas ou dados sensíveis em respostas
- Sempre valide dados de entrada
- Use HTTPS em todos os ambientes

## Esquema OpenAPI

```yaml
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
security:
  - bearerAuth: []
```
