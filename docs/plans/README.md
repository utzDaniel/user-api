# Plans (docs/plans)

Este diretório contém planos de execução do projeto (arquivos Markdown).

Status key: `draft`, `approved`, `done`

## Planos disponíveis

| Arquivo | Status | Descrição |
|---------|--------|-----------|
| [2026-04-26-setup-completo.md](2026-04-26-setup-completo.md) | `done` | Setup completo da user-api: scaffold Maven, configs, segurança, Docker, Keycloak e OpenAPI |
| [2026-05-03-endpoint-perfil.md](2026-05-03-endpoint-perfil.md) | `done` | Módulo Perfil + Família + Migração Roles: role-based auth, profile CRUD sync Keycloak, family/invitation flow |
| [2026-05-25-refatoracao.md](2026-05-25-refatoracao.md) | `done` | Refatoração da arquitetura: eliminação do módulo profile, acesso direto ao Keycloak via JDBC, simplificação do módulo family sem sistema de convites, implementação de KeycloakAdminClient |

## Como usar

- Crie/edite planos e abra PRs para revisão.
- Nomeie planos com data ou número para ordenação (ex: `2026-04-26-infra-setup.md`).
