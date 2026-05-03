package br.com.user.modules.family;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitationEntity, Long> {

    List<FamilyInvitationEntity> findAllByReceiverEmailAndStatus(String receiverEmail, InvitationStatusEnum status);

    List<FamilyInvitationEntity> findAllByFamilyId(Long familyId);
}
