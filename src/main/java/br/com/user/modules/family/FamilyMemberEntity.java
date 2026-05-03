package br.com.user.modules.family;

import br.com.user.modules.profile.ProfileEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "tb_family_member")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private FamilyEntity family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private ProfileEntity profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "parentesco", nullable = false)
    private ParentescoEnum parentesco;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FamilyMemberStatusEnum status;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void prePersist() {
        joinedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
