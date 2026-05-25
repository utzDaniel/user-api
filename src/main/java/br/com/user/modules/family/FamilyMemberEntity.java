package br.com.user.modules.family;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "FAMILY_MEMBER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "FAMILY_ID", nullable = false)
    private Long familyId;

    @Column(name = "USER_ID", nullable = false, unique = true, length = 36)
    private String userId;

    @Column(name = "JOINED_AT", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void prePersist() {
        joinedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
