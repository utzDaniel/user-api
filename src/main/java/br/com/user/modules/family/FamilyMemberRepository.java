package br.com.user.modules.family;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMemberEntity, Long> {

    boolean existsByProfileId(Long profileId);

    Optional<FamilyMemberEntity> findByProfileId(Long profileId);

    List<FamilyMemberEntity> findAllByFamilyId(Long familyId);
}
