package br.com.user.modules.family;

import br.com.user.modules.family.dto.*;
import br.com.user.modules.profile.ProfileEntity;
import br.com.user.modules.profile.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyInvitationRepository invitationRepository;
    private final ProfileRepository profileRepository;
    private final FamilyEventPublisher eventPublisher;

    public FamilyService(FamilyRepository familyRepository,
                         FamilyMemberRepository familyMemberRepository,
                         FamilyInvitationRepository invitationRepository,
                         ProfileRepository profileRepository,
                         FamilyEventPublisher eventPublisher) {
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.invitationRepository = invitationRepository;
        this.profileRepository = profileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public FamilyResponse createFamily(Long profileId, String nome) {
        if (familyMemberRepository.existsByProfileId(profileId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário já pertence a uma família");
        }

        ProfileEntity titular = findProfile(profileId);

        FamilyEntity family = FamilyEntity.builder()
                .titular(titular)
                .nome(nome)
                .build();
        family = familyRepository.save(family);

        FamilyMemberEntity member = FamilyMemberEntity.builder()
                .family(family)
                .profile(titular)
                .parentesco(ParentescoEnum.TITULAR)
                .status(FamilyMemberStatusEnum.ATIVO)
                .build();
        familyMemberRepository.save(member);

        eventPublisher.publishFamilyCreated(family.getId(), titular.getUsername());
        return toFamilyResponse(family);
    }

    public FamilyResponse getFamily(Long profileId) {
        FamilyMemberEntity membership = familyMemberRepository.findByProfileId(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Família não encontrada"));
        return toFamilyResponse(membership.getFamily());
    }

    @Transactional
    public FamilyMemberResponse updateFamilyMember(Long callerProfileId, Long memberId, FamilyUpdateMemberRequest request) {
        FamilyMemberEntity member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));

        boolean isTitular = member.getFamily().getTitular().getId().equals(callerProfileId);
        boolean isOwnRecord = member.getProfile().getId().equals(callerProfileId);
        if (!isTitular && !isOwnRecord) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        if (request.getParentesco() != null) member.setParentesco(request.getParentesco());
        if (request.getStatus() != null) member.setStatus(request.getStatus());

        FamilyMemberEntity saved = familyMemberRepository.save(member);
        eventPublisher.publishMemberUpdated(saved.getId(), saved.getProfile().getUsername());
        return toMemberResponse(saved);
    }

    @Transactional
    public void removeFamilyMember(Long callerProfileId, Long memberId) {
        FamilyMemberEntity member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));

        if (!member.getFamily().getTitular().getId().equals(callerProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o titular pode remover membros");
        }

        String username = member.getProfile().getUsername();
        familyMemberRepository.delete(member);
        eventPublisher.publishMemberRemoved(memberId, username);
    }

    @Transactional
    public FamilyInvitationResponse requestInvitation(Long profileId, FamilyInvitationSendRequest request) {
        FamilyMemberEntity membership = familyMemberRepository.findByProfileId(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não pertence a uma família"));

        ProfileEntity requester = findProfile(profileId);

        FamilyInvitationEntity invitation = FamilyInvitationEntity.builder()
                .family(membership.getFamily())
                .requester(requester)
                .receiverEmail(request.getReceiverEmail())
                .parentesco(request.getParentesco())
                .status(InvitationStatusEnum.AGUARDANDO_TITULAR)
                .build();
        invitation = invitationRepository.save(invitation);

        eventPublisher.publishInvitationRequested(invitation.getId(), requester.getUsername());
        return toInvitationResponse(invitation);
    }

    @Transactional
    public FamilyInvitationResponse approveInvitation(Long titularProfileId, Long invitationId) {
        FamilyInvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado"));

        if (!invitation.getFamily().getTitular().getId().equals(titularProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o titular pode aprovar convites");
        }

        invitation.setStatus(InvitationStatusEnum.PENDENTE);
        FamilyInvitationEntity saved = invitationRepository.save(invitation);

        eventPublisher.publishInvitationApprovedByTitular(saved.getId(), invitation.getFamily().getTitular().getUsername());
        return toInvitationResponse(saved);
    }

    @Transactional
    public FamilyInvitationResponse rejectInvitationByTitular(Long titularProfileId, Long invitationId) {
        FamilyInvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado"));

        if (!invitation.getFamily().getTitular().getId().equals(titularProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o titular pode recusar convites");
        }

        invitation.setStatus(InvitationStatusEnum.RECUSADO);
        FamilyInvitationEntity saved = invitationRepository.save(invitation);

        eventPublisher.publishInvitationRejectedByTitular(saved.getId(), invitation.getFamily().getTitular().getUsername());
        return toInvitationResponse(saved);
    }

    public List<FamilyInvitationResponse> listReceivedInvitations(String email) {
        return invitationRepository
                .findAllByReceiverEmailAndStatus(email, InvitationStatusEnum.PENDENTE)
                .stream().map(this::toInvitationResponse).collect(Collectors.toList());
    }

    public List<FamilyInvitationResponse> listSentInvitations(Long familyId) {
        return invitationRepository.findAllByFamilyId(familyId)
                .stream().map(this::toInvitationResponse).collect(Collectors.toList());
    }

    @Transactional
    public FamilyInvitationResponse acceptInvitation(Long invitationId, Long receiverProfileId) {
        FamilyInvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado"));

        if (invitation.getStatus() != InvitationStatusEnum.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Convite não está pendente");
        }

        if (familyMemberRepository.existsByProfileId(receiverProfileId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário já pertence a uma família");
        }

        ProfileEntity receiver = findProfile(receiverProfileId);

        FamilyMemberEntity member = FamilyMemberEntity.builder()
                .family(invitation.getFamily())
                .profile(receiver)
                .parentesco(invitation.getParentesco())
                .status(FamilyMemberStatusEnum.ATIVO)
                .build();
        familyMemberRepository.save(member);

        invitation.setStatus(InvitationStatusEnum.ACEITO);
        FamilyInvitationEntity saved = invitationRepository.save(invitation);

        eventPublisher.publishInvitationAccepted(saved.getId(), receiver.getUsername());
        return toInvitationResponse(saved);
    }

    @Transactional
    public FamilyInvitationResponse rejectInvitation(Long invitationId, Long receiverProfileId) {
        FamilyInvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado"));

        ProfileEntity receiver = findProfile(receiverProfileId);

        invitation.setStatus(InvitationStatusEnum.RECUSADO);
        FamilyInvitationEntity saved = invitationRepository.save(invitation);

        eventPublisher.publishInvitationRejected(saved.getId(), receiver.getUsername());
        return toInvitationResponse(saved);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ProfileEntity findProfile(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado"));
    }

    private FamilyResponse toFamilyResponse(FamilyEntity family) {
        List<FamilyMemberEntity> members = familyMemberRepository.findAllByFamilyId(family.getId());
        FamilyMemberResponse titularResponse = members.stream()
                .filter(m -> m.getParentesco() == ParentescoEnum.TITULAR)
                .findFirst()
                .map(this::toMemberResponse)
                .orElse(null);
        List<FamilyMemberResponse> outros = members.stream()
                .filter(m -> m.getParentesco() != ParentescoEnum.TITULAR)
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
        return FamilyResponse.builder()
                .id(family.getId())
                .nome(family.getNome())
                .titular(titularResponse)
                .membros(outros)
                .build();
    }

    private FamilyMemberResponse toMemberResponse(FamilyMemberEntity m) {
        return FamilyMemberResponse.builder()
                .id(m.getId())
                .nomeCompleto(m.getProfile().getNomeCompleto())
                .email(m.getProfile().getEmail())
                .parentesco(m.getParentesco())
                .status(m.getStatus())
                .build();
    }

    private FamilyInvitationResponse toInvitationResponse(FamilyInvitationEntity inv) {
        return FamilyInvitationResponse.builder()
                .id(inv.getId())
                .requesterNome(inv.getRequester().getNomeCompleto())
                .receiverEmail(inv.getReceiverEmail())
                .parentesco(inv.getParentesco())
                .status(inv.getStatus())
                .createdAt(inv.getCreatedAt())
                .build();
    }
}
