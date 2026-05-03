package br.com.user.modules.family.service;

import br.com.user.modules.family.*;
import br.com.user.modules.family.dto.*;
import br.com.user.modules.profile.ProfileEntity;
import br.com.user.modules.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private FamilyInvitationRepository invitationRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private FamilyEventPublisher eventPublisher;

    @InjectMocks
    private FamilyService familyService;

    private ProfileEntity titular;
    private ProfileEntity membro;
    private FamilyEntity family;
    private FamilyMemberEntity titularMember;
    private FamilyInvitationEntity invitation;

    @BeforeEach
    void setUp() {
        titular = ProfileEntity.builder()
                .id(1L)
                .username("keycloak-titular")
                .nomeCompleto("Titular Silva")
                .email("titular@email.com")
                .build();

        membro = ProfileEntity.builder()
                .id(2L)
                .username("keycloak-membro")
                .nomeCompleto("Membro Souza")
                .email("membro@email.com")
                .build();

        family = FamilyEntity.builder()
                .id(10L)
                .titular(titular)
                .createdAt(LocalDateTime.now())
                .build();

        titularMember = FamilyMemberEntity.builder()
                .id(100L)
                .family(family)
                .profile(titular)
                .parentesco(ParentescoEnum.TITULAR)
                .status(FamilyMemberStatusEnum.ATIVO)
                .joinedAt(LocalDateTime.now())
                .build();

        invitation = FamilyInvitationEntity.builder()
                .id(200L)
                .family(family)
                .requester(titular)
                .receiverEmail("novo@email.com")
                .parentesco(ParentescoEnum.CONJUGE)
                .status(InvitationStatusEnum.AGUARDANDO_TITULAR)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createFamilyShouldCreateFamilyAndAddTitularMember() {
        when(familyMemberRepository.existsByProfileId(1L)).thenReturn(false);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(titular));
        when(familyRepository.save(any(FamilyEntity.class))).thenReturn(family);
        when(familyMemberRepository.save(any(FamilyMemberEntity.class))).thenReturn(titularMember);
        when(familyMemberRepository.findAllByFamilyId(10L)).thenReturn(List.of(titularMember));

        FamilyResponse result = familyService.createFamily(1L, "teste");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitular()).isNotNull();
        verify(familyRepository).save(any(FamilyEntity.class));
        verify(familyMemberRepository).save(any(FamilyMemberEntity.class));
        verify(eventPublisher).publishFamilyCreated(10L, "keycloak-titular");
    }

    @Test
    void createFamilyShouldThrowWhenUserAlreadyHasFamily() {
        when(familyMemberRepository.existsByProfileId(1L)).thenReturn(true);

        assertThatThrownBy(() -> familyService.createFamily(1L, "teste"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(familyRepository, never()).save(any());
        verify(eventPublisher, never()).publishFamilyCreated(any(), any());
    }

    @Test
    void requestInvitationShouldCreatePendingApprovalInvitation() {
        FamilyInvitationSendRequest request = new FamilyInvitationSendRequest();
        request.setReceiverEmail("novo@email.com");
        request.setParentesco(ParentescoEnum.CONJUGE);

        when(familyMemberRepository.findByProfileId(1L)).thenReturn(Optional.of(titularMember));
        when(profileRepository.findById(1L)).thenReturn(Optional.of(titular));
        when(invitationRepository.save(any(FamilyInvitationEntity.class))).thenReturn(invitation);

        FamilyInvitationResponse result = familyService.requestInvitation(1L, request);

        assertThat(result.getId()).isEqualTo(200L);
        verify(invitationRepository).save(any(FamilyInvitationEntity.class));
        verify(eventPublisher).publishInvitationRequested(200L, "keycloak-titular");
    }

    @Test
    void requestInvitationShouldThrowWhenReceiverNotFound() {
        FamilyInvitationSendRequest request = new FamilyInvitationSendRequest();
        request.setReceiverEmail("novo@email.com");
        request.setParentesco(ParentescoEnum.CONJUGE);

        when(familyMemberRepository.findByProfileId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.requestInvitation(99L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void approveInvitationShouldChangeToPendingAndPublishEvent() {
        invitation.setStatus(InvitationStatusEnum.AGUARDANDO_TITULAR);
        FamilyInvitationEntity approved = FamilyInvitationEntity.builder()
                .id(200L).family(family).requester(titular)
                .receiverEmail("novo@email.com").parentesco(ParentescoEnum.CONJUGE)
                .status(InvitationStatusEnum.PENDENTE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(invitationRepository.findById(200L)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any())).thenReturn(approved);

        FamilyInvitationResponse result = familyService.approveInvitation(1L, 200L);

        assertThat(result.getStatus()).isEqualTo(InvitationStatusEnum.PENDENTE);
        verify(eventPublisher).publishInvitationApprovedByTitular(200L, "keycloak-titular");
    }

    @Test
    void approveInvitationShouldThrowWhenCallerIsNotTitular() {
        when(invitationRepository.findById(200L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> familyService.approveInvitation(99L, 200L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(eventPublisher, never()).publishInvitationApprovedByTitular(any(), any());
    }

    @Test
    void rejectInvitationByTitularShouldUpdateStatus() {
        FamilyInvitationEntity rejected = FamilyInvitationEntity.builder()
                .id(200L).family(family).requester(titular)
                .receiverEmail("novo@email.com").parentesco(ParentescoEnum.CONJUGE)
                .status(InvitationStatusEnum.RECUSADO)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(invitationRepository.findById(200L)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any())).thenReturn(rejected);

        FamilyInvitationResponse result = familyService.rejectInvitationByTitular(1L, 200L);

        assertThat(result.getStatus()).isEqualTo(InvitationStatusEnum.RECUSADO);
        verify(eventPublisher).publishInvitationRejectedByTitular(200L, "keycloak-titular");
    }

    @Test
    void acceptInvitationShouldCreateMemberAndPublishEvent() {
        invitation.setStatus(InvitationStatusEnum.PENDENTE);
        FamilyInvitationEntity accepted = FamilyInvitationEntity.builder()
                .id(200L).family(family).requester(titular)
                .receiverEmail("membro@email.com").parentesco(ParentescoEnum.CONJUGE)
                .status(InvitationStatusEnum.ACEITO)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        FamilyMemberEntity newMember = FamilyMemberEntity.builder()
                .id(101L).family(family).profile(membro)
                .parentesco(ParentescoEnum.CONJUGE).status(FamilyMemberStatusEnum.ATIVO)
                .joinedAt(LocalDateTime.now()).build();

        when(invitationRepository.findById(200L)).thenReturn(Optional.of(invitation));
        when(familyMemberRepository.existsByProfileId(2L)).thenReturn(false);
        when(profileRepository.findById(2L)).thenReturn(Optional.of(membro));
        when(familyMemberRepository.save(any())).thenReturn(newMember);
        when(invitationRepository.save(any())).thenReturn(accepted);

        FamilyInvitationResponse result = familyService.acceptInvitation(200L, 2L);

        assertThat(result.getStatus()).isEqualTo(InvitationStatusEnum.ACEITO);
        verify(familyMemberRepository).save(any(FamilyMemberEntity.class));
        verify(eventPublisher).publishInvitationAccepted(200L, "keycloak-membro");
    }

    @Test
    void acceptInvitationShouldThrowWhenReceiverAlreadyHasFamily() {
        invitation.setStatus(InvitationStatusEnum.PENDENTE);

        when(invitationRepository.findById(200L)).thenReturn(Optional.of(invitation));
        when(familyMemberRepository.existsByProfileId(2L)).thenReturn(true);

        assertThatThrownBy(() -> familyService.acceptInvitation(200L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(familyMemberRepository, never()).save(any());
        verify(eventPublisher, never()).publishInvitationAccepted(any(), any());
    }

    @Test
    void acceptInvitationShouldThrowWhenInvitationNotPending() {
        invitation.setStatus(InvitationStatusEnum.AGUARDANDO_TITULAR);

        when(invitationRepository.findById(200L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> familyService.acceptInvitation(200L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(familyMemberRepository, never()).save(any());
    }

    @Test
    void rejectInvitationShouldUpdateStatusAndPublishEvent() {
        FamilyInvitationEntity rejected = FamilyInvitationEntity.builder()
                .id(200L).family(family).requester(titular)
                .receiverEmail("membro@email.com").parentesco(ParentescoEnum.CONJUGE)
                .status(InvitationStatusEnum.RECUSADO)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(invitationRepository.findById(200L)).thenReturn(Optional.of(invitation));
        when(profileRepository.findById(2L)).thenReturn(Optional.of(membro));
        when(invitationRepository.save(any())).thenReturn(rejected);

        FamilyInvitationResponse result = familyService.rejectInvitation(200L, 2L);

        assertThat(result.getStatus()).isEqualTo(InvitationStatusEnum.RECUSADO);
        verify(eventPublisher).publishInvitationRejected(200L, "keycloak-membro");
    }

    @Test
    void removeFamilyMemberShouldThrowWhenCallerIsNotTitular() {
        FamilyMemberEntity memberToRemove = FamilyMemberEntity.builder()
                .id(101L).family(family).profile(membro)
                .parentesco(ParentescoEnum.CONJUGE).status(FamilyMemberStatusEnum.ATIVO)
                .joinedAt(LocalDateTime.now()).build();

        when(familyMemberRepository.findById(101L)).thenReturn(Optional.of(memberToRemove));

        assertThatThrownBy(() -> familyService.removeFamilyMember(99L, 101L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(familyMemberRepository, never()).delete(any());
        verify(eventPublisher, never()).publishMemberRemoved(any(), any());
    }
}
