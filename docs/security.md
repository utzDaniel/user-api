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
| /api/v1/users | ADMIN |
| /api/v1/users/{id} | Autenticado (próprio usuário ou ADMIN) |
| /api/v1/users/{id}/roles | ADMIN |
| /api/v1/public/** | Público |
| /swagger-ui/** | Público |
| /actuator/health | Público |
| /actuator/info | Público |

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
