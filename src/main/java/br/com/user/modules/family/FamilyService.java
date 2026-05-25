package br.com.user.modules.family;

import br.com.user.config.ApiException;
import br.com.user.config.KeycloakConfig;
import br.com.user.modules.event.EventPublisher;
import br.com.user.modules.event.EventType;
import br.com.user.modules.family.dto.FamilyMemberResponse;
import br.com.user.modules.family.dto.FamilyResponse;
import br.com.user.modules.user.KeycloakUserDao;
import br.com.user.modules.user.dto.KeycloakUserDto;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final EventPublisher eventPublisher;
    private final KeycloakUserDao keycloakUserDao;
    private final KeycloakConfig keycloakConfig;


    public FamilyService(FamilyRepository familyRepository,
                         FamilyMemberRepository familyMemberRepository,
                         EventPublisher eventPublisher,
                         KeycloakUserDao keycloakUserDao,
                         KeycloakConfig keycloakConfig) {

        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.eventPublisher = eventPublisher;
        this.keycloakUserDao = keycloakUserDao;
        this.keycloakConfig = keycloakConfig;
    }

    private KeycloakUserDto getKeycloakUserDto(Jwt jwt) {
        return keycloakUserDao
                .findByRealmAndUsername(keycloakConfig.getRealm(), jwt.getClaimAsString("preferred_username"))
                .orElseThrow(() -> ApiException.notFound("Perfil não encontrado"));
    }

    @Transactional
    public FamilyResponse createFamily(Jwt jwt, String nome) {
        KeycloakUserDto user = getKeycloakUserDto(jwt);

        if (user.familyId() != null) {
            throw ApiException.badRequest("Usuário já pertence a uma família");
        }

        FamilyEntity family = FamilyEntity.builder()
                .holderId(user.id())
                .name(nome)
                .build();
        family = familyRepository.save(family);

        FamilyMemberEntity member = FamilyMemberEntity.builder()
                .familyId(family.getId())
                .userId(user.id())
                .build();

        familyMemberRepository.save(member);

        eventPublisher.publish(EventType.FAMILY_CREATED, user.id(), nome);

        return new FamilyResponse(
                family.getName(),
                true,
                List.of(new FamilyMemberResponse(
                        user.username(),
                        user.firstName(),
                        user.lastName(),
                        user.email(),
                        user.emailVerified(),
                        false
                ))
        );
    }

    public FamilyResponse getFamily(Jwt jwt) {
        KeycloakUserDto user = getKeycloakUserDto(jwt);

        if (user.familyId() == null) {
            throw ApiException.notFound("Usuário não pertence a uma família");
        }

        List<FamilyMemberResponse> membros = keycloakUserDao.findByFamilyId(user.familyId()).stream()
                .map(u -> new FamilyMemberResponse(
                        u.username(),
                        u.firstName(),
                        u.lastName(),
                        u.email(),
                        u.emailVerified(),
                        isDeleteable(user, u.username())
                ))
                .toList();

        return new FamilyResponse(
                user.familyName(),
                user.holder(),
                membros
        );
    }

    @Transactional
    public void removeFamilyMember(Jwt jwt, String username) {

        KeycloakUserDto user = getKeycloakUserDto(jwt);

        if (user.username().equals(username) && user.holder()) {
            throw ApiException.forbiden("O titular não pode remover a si mesmo");
        }

        if (!user.username().equals(username) && !user.holder()) {
            throw ApiException.forbiden("Somente o titular pode remover outros membros");
        }

        FamilyMemberEntity member = familyMemberRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Membro não encontrado"));

        familyMemberRepository.delete(member);

        eventPublisher.publish(EventType.FAMILY_MEMBER_REMOVED, user.id(), username);
    }

    @Transactional
    public void addFamilyMember(Jwt jwt, String username) {
        KeycloakUserDto holder = getKeycloakUserDto(jwt);

        if (holder.familyId() == null) {
            throw ApiException.badRequest("Usuário não pertence a uma família");
        }

        if (!holder.holder()) {
            throw ApiException.forbiden("Somente o titular pode adicionar membros");
        }

        KeycloakUserDto newMember = keycloakUserDao
                .findByRealmAndUsername(keycloakConfig.getRealm(), username)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));

        if (newMember.familyId() != null) {
            throw ApiException.badRequest("Usuário já pertence a uma família");
        }

        FamilyMemberEntity member = FamilyMemberEntity.builder()
                .familyId(holder.familyId())
                .userId(newMember.id())
                .build();

        familyMemberRepository.save(member);

        eventPublisher.publish(EventType.FAMILY_MEMBER_ADDED, holder.id(), username);
    }

    @Transactional
    public void deleteFamily(Jwt jwt) {
        KeycloakUserDto user = getKeycloakUserDto(jwt);

        if (user.familyId() == null) {
            throw ApiException.badRequest("Usuário não pertence a uma família");
        }

        if (!user.holder()) {
            throw ApiException.forbiden("Somente o titular pode remover a família");
        }

        familyRepository.findById(user.familyId())
                .orElseThrow(() -> ApiException.notFound("Família não encontrada"));

        familyMemberRepository.deleteByFamilyId(user.familyId());

        familyRepository.deleteById(user.familyId());

        eventPublisher.publish(EventType.FAMILY_DELETED, user.id(), user.familyName());
    }

    private boolean isDeleteable(KeycloakUserDto user, String username) {
        return user.holder() != user.username().equals(username);
    }
}
