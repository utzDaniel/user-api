# Arquitetura

## Estilo

API REST em camadas, orientada a eventos. Pacote raiz: `br.com.user`.

## Camadas

```
Controller -> Service -> Repository
                |
          EventPublisher
```

- **Controller** — recebe HTTP, delega ao Service, sem lógica de negócio
- **Service** — lógica de negócio, publica eventos após escrita
- **Repository** — apenas persistência JPA
- **EventPublisher** — publica na fila ActiveMQ

## Módulos

| Módulo | Pacote | Responsabilidade |
|--------|--------|------------------|
| users | `br.com.user.modules.user` | Gestão de usuários (criação, atualização, remoção) |
| shared | `br.com.user.shared` | Exceções, modelos de resposta, utilitários |

## Dependências Externas

| Serviço | Função | Porta |
|---------|--------|-------|
| SQL Server | Persistência | 1433 |
| ActiveMQ | Broker de eventos | 61616 |
| Keycloak | IdP — emite JWT | 8081 |

## Diagrama

```
 Cliente HTTP
     | Bearer JWT
     v
 gateway-api (valida JWT + roteia)
     |
     v
 user-api (Resource Server OAuth2)
  +-- valida JWT --> Keycloak :8081
  +-- persiste   --> SQL Server :1433
  +-- publica    --> ActiveMQ :61616
```

## Segurança e Versionamento

- Resource Server OAuth2 — valida JWT via Keycloak; todos os endpoints protegidos por padrão
- URL versionada: `/api/v1/...` — nova versão apenas em breaking change
