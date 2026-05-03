package br.com.user.modules.profile;

import br.com.user.modules.profile.dto.PasswordChangeRequest;
import br.com.user.modules.profile.dto.ProfileResponse;
import br.com.user.modules.profile.dto.ProfileUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile", description = "Gerenciamento do perfil do usuário autenticado")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Obtém o perfil do usuário autenticado",
               description = "Retorna os dados do perfil. Se ainda não existir, provisiona automaticamente a partir das claims do JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.getProfile(jwt));
    }

    @PutMapping
    @Operation(summary = "Atualiza o perfil do usuário autenticado",
               description = "Atualiza nome, email e telefone. Sincroniza nome e email com o Keycloak via Admin API. Publica o evento PROFILE_UPDATED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado"),
        @ApiResponse(responseCode = "404", description = "Perfil não encontrado"),
        @ApiResponse(responseCode = "502", description = "Falha ao sincronizar com o Keycloak")
    })
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                                         @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(jwt, request));
    }

    @PutMapping("/password")
    @Operation(summary = "Altera a senha do usuário autenticado",
               description = "Valida a senha atual via ROPC e redefine a nova senha via Admin API do Keycloak. Publica o evento PROFILE_PASSWORD_CHANGED.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Senha atual inválida ou senhas não conferem"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado"),
        @ApiResponse(responseCode = "502", description = "Falha ao redefinir senha no Keycloak")
    })
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @RequestBody PasswordChangeRequest request) {
        profileService.changePassword(jwt, request);
        return ResponseEntity.noContent().build();
    }
}
