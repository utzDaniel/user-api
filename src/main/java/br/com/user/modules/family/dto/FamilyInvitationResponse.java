package br.com.user.modules.family.dto;

import br.com.user.modules.family.InvitationStatusEnum;
import br.com.user.modules.family.ParentescoEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FamilyInvitationResponse {
    private Long id;
    private String requesterNome;
    private String receiverEmail;
    private ParentescoEnum parentesco;
    private InvitationStatusEnum status;
    private LocalDateTime createdAt;
}
