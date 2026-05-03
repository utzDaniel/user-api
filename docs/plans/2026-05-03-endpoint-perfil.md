# Plano: Módulo Perfil + Família + Migração Roles

Status: done

Autor: Daniel

Data: 2026-05-03

## TL;DR

Migrar segurança de scope-based (`user.read/write`) para role-based (`ROLE_USER` / `ROLE_ADMIN`); criar migration SQL com `tb_profile`, `tb_family`, `tb_family_member` e `tb_family_invitation`; implementar módulo `profile` com auto-provisioning, sync bidirecional de nome/email com Keycloak via Admin API e troca de senha via ROPC; implementar módulo `family` com regra de família única por usuário, fluxo de convite em duas etapas (membro solicita → TITULAR aprova → destinatário aceita/recusa) e proteção contra entrada em múltiplas famílias; documentar todos os endpoints com OpenAPI.

## Contexto
- Imagem de referência: docs/images/exemplo-tela-Inicial-perfil.png
- Sem módulos implementados ainda (só config)
- V1__init.sql = apenas SELECT (placeholder)
- SecurityConfig usa scope-based → migrar para roles
- Decisions: senha via Keycloak Admin API, familiares = usuários existentes convidados, migrar TODOS os endpoints para roles

---

## Steps

### Phase 1 — Migração de Segurança

Arquivo: src/main/java/br/com/user/config/SecurityConfig.java
- Substituir JwtGrantedAuthoritiesConverter por converter custom que lê realm_access.roles do JWT e prefixa com ROLE_
- Regras: /api/v1/users/** → hasRole("ADMIN") | /api/v1/profile/** → hasAnyRole("USER", "ADMIN")

Arquivo: src/test/java/br/com/user/config/SecurityConfigTest.java
- Atualizar testes que usam user.read/write para ROLE_USER/ROLE_ADMIN
- Adicionar testes para endpoints de perfil

---

### Phase 2 — Migration SQL (V2__profile.sql)

Tabelas:
- tb_profile: id, keycloak_id (UNIQUE), nome_completo, cpf (UNIQUE), email (UNIQUE), telefone, created_at, updated_at
- tb_family: id, titular_profile_id FK (NOT NULL), created_at — representa a família criada por um titular
- tb_family_member: id, family_id FK, profile_id FK (UNIQUE — garante 1 família por usuário), parentesco, status (ATIVO/INATIVO), joined_at — UNIQUE(family_id, profile_id)
- tb_family_invitation: id, family_id FK, requester_profile_id FK, receiver_email, parentesco, status (AGUARDANDO_TITULAR/PENDENTE/ACEITO/RECUSADO), created_at, updated_at

Obs: titular_profile_id em tb_family também referencia tb_family_member implicitamente; profile_id UNIQUE em tb_family_member garante 1 família por usuário

---

### Phase 3 — Módulo Profile (paralelo com Phase 4)

Config:
- config/KeycloakAdminConfig.java — RestClient apontando para Keycloak Admin API
- application.yml — adicionar keycloak.admin.server-url, keycloak.admin.realm, keycloak.admin.client-id, keycloak.admin.client-secret

Classes em br.com.user.modules.profile:
- ProfileEntity, ProfileRepository, ProfileMapper
- ProfileResponse, ProfileUpdateRequest (nomeCompleto, email, telefone), PasswordChangeRequest (senhaAtual, novaSenha, confirmarNovaSenha)
- ProfileEventPublisher — PROFILE_UPDATED, PROFILE_PASSWORD_CHANGED
- ProfileService:
  - getProfile(String keycloakId) — busca ou auto-provisiona a partir de claims JWT
  - updateProfile(String keycloakId, ProfileUpdateRequest) — salva no banco + chama Admin API para atualizar firstName/lastName/email no Keycloak → publica PROFILE_UPDATED
  - changePassword(String keycloakId, PasswordChangeRequest) — valida atual via ROPC + chama Admin API reset → publica PROFILE_PASSWORD_CHANGED
- ProfileController:
  - GET /api/v1/profile
  - PUT /api/v1/profile
  - PUT /api/v1/profile/password

Regra de sync Keycloak:
- PUT /api/v1/profile sincroniza nome e email no Keycloak via Admin API (PATCH /admin/realms/{realm}/users/{id})
- Se Keycloak retornar erro → rollback local (transação Spring) + retornar 502

Testes ProfileServiceTest (9 cenários):
- getProfileShouldReturnExistingProfile
- getProfileShouldAutoProvisionFromJwtClaims
- updateProfileShouldSaveAndPublishEvent
- updateProfileShouldSyncNameAndEmailToKeycloak
- updateProfileShouldRollbackWhenKeycloakSyncFails
- updateProfileShouldThrowWhenProfileNotFound
- changePasswordShouldCallKeycloakAndPublishEvent
- changePasswordShouldThrowWhenCurrentPasswordInvalid
- changePasswordShouldThrowWhenPasswordsDoNotMatch

---

### Phase 4 — Módulo Family (paralelo com Phase 3)

Regras de negócio críticas:
1. Um usuário só pode pertencer a UMA família (garantido por profile_id UNIQUE em tb_family_member)
2. Ao aceitar um convite, verificar se já pertence a uma família → lançar exceção
3. Somente o TITULAR pode criar a família (ao criar ele é adicionado como membro com parentesco TITULAR)
4. Membros podem solicitar convite, mas o TITULAR deve aprovar antes do convite ser enviado ao usuário alvo
   - Fluxo: membro solicita → status AGUARDANDO_TITULAR → TITULAR aprova → status PENDENTE → usuário alvo aceita/recusa

Enums:
- ParentescoEnum: TITULAR, CONJUGE, FILHO, FILHA, PAI, MAE, IRMAO, IRMA, OUTRO
- FamilyMemberStatusEnum: ATIVO, INATIVO
- InvitationStatusEnum: AGUARDANDO_TITULAR, PENDENTE, ACEITO, RECUSADO

Entities: FamilyEntity, FamilyMemberEntity, FamilyInvitationEntity
Repositories: FamilyRepository, FamilyMemberRepository (existsByProfileId), FamilyInvitationRepository

DTOs:
- FamilyResponse (titular + lista de membros)
- FamilyMemberResponse (nomeCompleto, email, parentesco, status)
- FamilyInvitationSendRequest (receiverEmail, parentesco)
- FamilyInvitationResponse (id, requesterNome, receiverEmail, parentesco, status, createdAt)
- FamilyUpdateMemberRequest (parentesco, status)

Events: FamilyEventPublisher — FAMILY_CREATED, FAMILY_INVITATION_REQUESTED, FAMILY_INVITATION_APPROVED_BY_TITULAR, FAMILY_INVITATION_REJECTED_BY_TITULAR, FAMILY_INVITATION_ACCEPTED, FAMILY_INVITATION_REJECTED, FAMILY_MEMBER_REMOVED, FAMILY_MEMBER_UPDATED

FamilyService:
- createFamily(Long profileId) — cria família + adiciona como TITULAR → lança exceção se já tem família → publica FAMILY_CREATED
- getFamily(Long profileId) — retorna família do usuário
- updateFamilyMember(Long profileId, Long memberId, request) → publica FAMILY_MEMBER_UPDATED (somente TITULAR ou próprio membro)
- removeFamilyMember(Long profileId, Long memberId) → somente TITULAR → publica FAMILY_MEMBER_REMOVED
- requestInvitation(Long profileId, request) — qualquer membro solicita → status AGUARDANDO_TITULAR → publica FAMILY_INVITATION_REQUESTED
- approveInvitation(Long titularProfileId, Long invitationId) — somente TITULAR → muda para PENDENTE → publica FAMILY_INVITATION_APPROVED_BY_TITULAR
- rejectInvitationByTitular(Long titularProfileId, Long invitationId) → somente TITULAR → status RECUSADO → publica FAMILY_INVITATION_REJECTED_BY_TITULAR
- listReceivedInvitations(String email) — convites PENDENTE para o email do usuário
- listSentInvitations(Long familyId) — convites da família
- acceptInvitation(Long invitationId, Long receiverProfileId) — verifica se receiver já tem família → lança exceção → cria FamilyMember + atualiza convite → publica FAMILY_INVITATION_ACCEPTED
- rejectInvitation(Long invitationId, Long receiverProfileId) → status RECUSADO → publica FAMILY_INVITATION_REJECTED

FamilyController:
- POST /api/v1/profile/family — createFamily
- GET /api/v1/profile/family — getFamily
- PUT /api/v1/profile/family/members/{id} — updateFamilyMember
- DELETE /api/v1/profile/family/members/{id} — removeFamilyMember
- POST /api/v1/profile/family/invitations — requestInvitation (qualquer membro)
- PUT /api/v1/profile/family/invitations/{id}/approve — approveInvitation (somente TITULAR)
- PUT /api/v1/profile/family/invitations/{id}/reject-titular — rejectInvitationByTitular (somente TITULAR)
- GET /api/v1/profile/family/invitations/received — listReceivedInvitations
- GET /api/v1/profile/family/invitations/sent — listSentInvitations
- PUT /api/v1/profile/family/invitations/{id}/accept — acceptInvitation
- PUT /api/v1/profile/family/invitations/{id}/reject — rejectInvitation

Testes FamilyServiceTest (12 cenários):
- createFamilyShouldCreateFamilyAndAddTitularMember
- createFamilyShouldThrowWhenUserAlreadyHasFamily
- requestInvitationShouldCreatePendingApprovalInvitation
- requestInvitationShouldThrowWhenReceiverNotFound
- approveInvitationShouldChangeToPendingAndPublishEvent
- approveInvitationShouldThrowWhenCallerIsNotTitular
- rejectInvitationByTitularShouldUpdateStatus
- acceptInvitationShouldCreateMemberAndPublishEvent
- acceptInvitationShouldThrowWhenReceiverAlreadyHasFamily
- acceptInvitationShouldThrowWhenInvitationNotPending
- rejectInvitationShouldUpdateStatusAndPublishEvent
- removeFamilyMemberShouldThrowWhenCallerIsNotTitular

---

### Phase 5 — Docs e OpenAPI

- Anotar controllers com @Operation, @ApiResponse
- Atualizar docs/api.md, docs/security.md, docs/events.md, docs/plans/README.md

---

## Arquivos

| Arquivo | Ação |
|---------|------|
| config/SecurityConfig.java | Modificar |
| config/SecurityConfigTest.java | Modificar |
| config/KeycloakAdminConfig.java | Criar |
| db/migration/V2__profile.sql | Criar |
| application.yml | Modificar |
| modules/profile/** (9 classes) | Criar |
| modules/family/** (13 classes) | Criar |
| ProfileServiceTest.java | Criar |
| FamilyServiceTest.java | Criar |
| docs/api.md, security.md, events.md, plans/README.md | Modificar |

---

## Verificação

1. mvn clean test — todos os testes passam
2. GET /api/v1/profile com ROLE_USER → 200
3. GET /api/v1/profile com user.read (scope antigo) → 403
4. POST /api/v1/profile/family (2x mesmo usuário) → 2ª chamada retorna 409
5. Aceitar convite com usuário já em família → 409
6. Membro solicita convite → status AGUARDANDO_TITULAR → somente TITULAR aprova → status PENDENTE → alvo aceita → aparece na família
7. PUT /api/v1/profile → nome e email atualizados no Keycloak

---

## Decisões

- Senha: Keycloak Admin API (ROPC para validar atual + Admin reset para mudar)
- Nome/email em sync com Keycloak ao atualizar perfil
- Familiares: somente usuários existentes no sistema
- 1 família por usuário (UNIQUE em tb_family_member.profile_id)
- TITULAR deve aprovar convites antes de chegar ao destinatário
- Auto-provisioning do perfil no primeiro GET (a partir de claims JWT)
- /api/v1/users/** → somente ROLE_ADMIN
- /api/v1/profile/** → ROLE_USER ou ROLE_ADMIN
