package br.com.user.modules.family;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMemberEntity, Long> {

    @Query(value = "SELECT fm.* FROM FAMILY_MEMBER fm " +
            "INNER JOIN USER_ENTITY u ON fm.USER_ID = u.ID " +
            "WHERE u.USERNAME = :username", nativeQuery = true)
    Optional<FamilyMemberEntity> findByUsername(@Param("username") String username);

    @Modifying
    @Query(value = "DELETE FROM FAMILY_MEMBER WHERE FAMILY_ID = :familyId", nativeQuery = true)
    void deleteByFamilyId(@Param("familyId") Long familyId);

}
