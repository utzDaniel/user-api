package br.com.user.modules.user;

import br.com.user.config.ApiException;
import br.com.user.config.KeycloakAdminClient;
import br.com.user.config.KeycloakConfig;
import br.com.user.config.Violacao;
import br.com.user.modules.event.EventPublisher;
import br.com.user.modules.event.EventType;
import br.com.user.modules.family.dto.FamilyMemberResponse;
import br.com.user.modules.user.dto.KeycloakUserDto;
import br.com.user.modules.user.dto.PasswordChangeRequest;
import br.com.user.modules.user.dto.UserResponse;
import br.com.user.modules.user.dto.UserUpdateRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final KeycloakUserDao keycloakUserDao;
    private final EventPublisher eventPublisher;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakConfig keycloakConfig;

    public UserService(KeycloakUserDao keycloakUserDao,
                       EventPublisher eventPublisher,
                       KeycloakAdminClient keycloakAdminClient,
                       KeycloakConfig keycloakConfig) {
        this.keycloakUserDao = keycloakUserDao;
        this.eventPublisher = eventPublisher;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakConfig = keycloakConfig;
    }

    public UserResponse getUser(Jwt jwt) {
        KeycloakUserDto user = getKeycloakUserDto(jwt);
        return new UserResponse(
                user.firstName(),
                user.lastName(),
                user.email(),
                user.emailVerified()
        );
    }

    private KeycloakUserDto getKeycloakUserDto(Jwt jwt) {
        return keycloakUserDao
                .findByRealmAndUsername(keycloakConfig.getRealm(), jwt.getClaimAsString("preferred_username"))
                .orElseThrow(() -> ApiException.notFound("User não encontrado"));
    }

    @Transactional
    public UserResponse updateUser(Jwt jwt, UserUpdateRequest request) {
        KeycloakUserDto user = getKeycloakUserDto(jwt);

        boolean emailVerified = user.emailVerified();
        if (!request.email().equals(user.email())) {
            if (keycloakUserDao.existsByEmail(request.email())) {
                throw ApiException.badRequest(List.of(new Violacao("email", "E-mail já utilizado")));
            }
            emailVerified = false;
        }

        keycloakAdminClient.updateUser(user.id(), request.nome(), request.sobrenome(), request.email(), emailVerified);
        eventPublisher.publish(EventType.USER_UPDATED, user.id(), request);

        return new UserResponse(
                request.nome(),
                request.sobrenome(),
                request.email(),
                emailVerified
        );
    }

    @Transactional
    public void changePassword(Jwt jwt, PasswordChangeRequest request) {
        if (!request.novaSenha().equals(request.confirmarNovaSenha())) {
            throw ApiException.badRequest(List.of(
                    new Violacao("novaSenha", "Nova Senha diferente da confirmação"),
                    new Violacao("confirmarNovaSenha", "Confirmação de senha diferente da nova senha")));
        }

        KeycloakUserDto user = getKeycloakUserDto(jwt);
        keycloakAdminClient.validatePassword(user.username(), request.senhaAtual());
        keycloakAdminClient.resetPassword(user.id(), request.novaSenha());

        eventPublisher.publish(EventType.USER_PASSWORD_CHANGED, user.id(), user.username());
    }

    public List<FamilyMemberResponse> getUsersWithoutFamily() {
        return keycloakUserDao.findUsersWithoutFamily(keycloakConfig.getRealm())
                .stream()
                .map(user -> new FamilyMemberResponse(
                        user.username(),
                        user.firstName(),
                        user.lastName(),
                        user.email(),
                        user.emailVerified(),
                        true
                ))
                .toList();
    }
}
