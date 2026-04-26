# Swagger / OpenAPI

## Regras

- `docs/openapi/spec.json` é o contrato oficial da API (fonte da verdade)
- Todo endpoint DEVE ter: summary, description, request schema, response schema e exemplos
- Schemas devem ser reutilizáveis via `$ref` em `components/schemas`
- Nunca duplique definições de schema
- Gerar `spec.html` após alterações: executar `scripts/generate-docs.ps1`

## Estrutura Obrigatória

```yaml
openapi: 3.0.0
info: { title, version, description }
servers: [ { url } ]
tags: [ { name, description } ]
paths: { ... }
components: { schemas: { ... }, securitySchemes: { bearerAuth: ... } }
security: [ { bearerAuth: [] } ]
```

## Exemplo de Endpoint

```yaml
paths:
  /api/v1/users:
    post:
      tags: [Users]
      summary: Cria um usuário
      description: Cria um novo usuário e publica o evento USER_CREATED
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UsersCreateRequest'
            example:
              name: João Silva
              email: joao@email.com
      responses:
        '201':
          description: Usuário criado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UsersCreatedResponse'
              example:
                id: 42
                name: João Silva
                email: joao@email.com
        '401':
          description: Não autorizado
        '403':
          description: Acesso negado
```

## Antipadrões

- Schemas embutidos inline (sem `$ref`)
- Exemplos ausentes
