package br.com.user.modules.family;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FamilyRepository extends JpaRepository<FamilyEntity, Long> {

    Optional<FamilyEntity> findByTitularId(Long titularProfileId);
}
