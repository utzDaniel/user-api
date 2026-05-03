package br.com.user.modules.family;

import br.com.user.modules.family.dto.*;
import br.com.user.modules.profile.ProfileEntity;
import br.com.user.modules.profile.ProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile/family")
@Tag(name = "Family", description = "Gerenciamento de família do usuário autenticado")
@SecurityRequirement(name = "bearerAuth")
public class FamilyController {

    private final FamilyService familyService;
    private final ProfileRepository profileRepository;

    public FamilyController(FamilyService familyService, ProfileRepository profileRepository) {
        this.familyService = familyService;
        this.profileRepository = profileRepository;
    }

    private Long resolveProfileId(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return profileRepository.findByUsername(username)
                .map(ProfileEntity::getId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Perfil não encontrado para o usuário autenticado"));
    }

    @PostMapping
    @Operation(summary = "Cria uma família", description = "Cria uma nova família e adiciona o usuário autenticado como TITULAR. Publica o evento FAMILY_CREATED.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Família criada"),
        @ApiResponse(responseCode = "409", description = "Usuário já pertence a uma família"),
        @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    public ResponseEntity<FamilyResponse> createFamily(@AuthenticationPrincipal Jwt jwt,
                                                        @RequestBody CreateFamilyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(familyService.createFamily(resolveProfileId(jwt), request.getNome()));
    }

    @GetMapping
    @Operation(summary = "Obtém a família do usuário autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Família retornada"),
        @ApiResponse(responseCode = "404", description = "Família não encontrada"),
        @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    public ResponseEntity<FamilyResponse> getFamily(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(familyService.getFamily(resolveProfileId(jwt)));
    }

    @PutMapping("/members/{id}")
    @Operation(summary = "Atualiza dados de um membro da família. Publica FAMILY_MEMBER_UPDATED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Membro atualizado"),
        @ApiResponse(responseCode = "403", description = "Somente titular ou próprio membro"),
        @ApiResponse(responseCode = "404", description = "Membro não encontrado")
    })
    public ResponseEntity<FamilyMemberResponse> updateFamilyMember(@PathVariable Long id,
                                                                    @AuthenticationPrincipal Jwt jwt,
                                                                    @RequestBody FamilyUpdateMemberRequest request) {
        return ResponseEntity.ok(familyService.updateFamilyMember(resolveProfileId(jwt), id, request));
    }

    @DeleteMapping("/members/{id}")
    @Operation(summary = "Remove um membro da família (somente TITULAR). Publica FAMILY_MEMBER_REMOVED.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Membro removido"),
        @ApiResponse(responseCode = "403", description = "Somente titular pode remover"),
        @ApiResponse(responseCode = "404", description = "Membro não encontrado")
    })
    public ResponseEntity<Void> removeFamilyMember(@PathVariable Long id,
                                                    @AuthenticationPrincipal Jwt jwt) {
        familyService.removeFamilyMember(resolveProfileId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations")
    @Operation(summary = "Solicita um convite (qualquer membro). Status inicial: AGUARDANDO_TITULAR. Publica FAMILY_INVITATION_REQUESTED.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Convite solicitado"),
        @ApiResponse(responseCode = "404", description = "Usuário não pertence a uma família")
    })
    public ResponseEntity<FamilyInvitationResponse> requestInvitation(@AuthenticationPrincipal Jwt jwt,
                                                                       @RequestBody FamilyInvitationSendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(familyService.requestInvitation(resolveProfileId(jwt), request));
    }

    @PutMapping("/invitations/{id}/approve")
    @Operation(summary = "Titular aprova convite (AGUARDANDO_TITULAR → PENDENTE). Publica FAMILY_INVITATION_APPROVED_BY_TITULAR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Convite aprovado"),
        @ApiResponse(responseCode = "403", description = "Somente titular pode aprovar"),
        @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    })
    public ResponseEntity<FamilyInvitationResponse> approveInvitation(@PathVariable Long id,
                                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(familyService.approveInvitation(resolveProfileId(jwt), id));
    }

    @PutMapping("/invitations/{id}/reject-titular")
    @Operation(summary = "Titular recusa convite. Publica FAMILY_INVITATION_REJECTED_BY_TITULAR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Convite recusado pelo titular"),
        @ApiResponse(responseCode = "403", description = "Somente titular pode recusar"),
        @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    })
    public ResponseEntity<FamilyInvitationResponse> rejectInvitationByTitular(@PathVariable Long id,
                                                                                @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(familyService.rejectInvitationByTitular(resolveProfileId(jwt), id));
    }

    @GetMapping("/invitations/received")
    @Operation(summary = "Lista convites PENDENTE recebidos pelo e-mail do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de convites recebidos")
    public ResponseEntity<List<FamilyInvitationResponse>> listReceivedInvitations(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(familyService.listReceivedInvitations(email));
    }

    @GetMapping("/invitations/sent")
    @Operation(summary = "Lista convites enviados pela família do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de convites enviados")
    public ResponseEntity<List<FamilyInvitationResponse>> listSentInvitations(@AuthenticationPrincipal Jwt jwt) {
        FamilyResponse family = familyService.getFamily(resolveProfileId(jwt));
        return ResponseEntity.ok(familyService.listSentInvitations(family.getId()));
    }

    @PutMapping("/invitations/{id}/accept")
    @Operation(summary = "Destinatário aceita convite. Publica FAMILY_INVITATION_ACCEPTED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Convite aceito"),
        @ApiResponse(responseCode = "409", description = "Usuário já pertence a uma família ou convite não está pendente"),
        @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    })
    public ResponseEntity<FamilyInvitationResponse> acceptInvitation(@PathVariable Long id,
                                                                       @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(familyService.acceptInvitation(id, resolveProfileId(jwt)));
    }

    @PutMapping("/invitations/{id}/reject")
    @Operation(summary = "Destinatário recusa convite. Publica FAMILY_INVITATION_REJECTED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Convite recusado"),
        @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    })
    public ResponseEntity<FamilyInvitationResponse> rejectInvitation(@PathVariable Long id,
                                                                       @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(familyService.rejectInvitation(id, resolveProfileId(jwt)));
    }
}
