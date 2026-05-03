package br.com.user.modules.family.dto;

import br.com.user.modules.family.ParentescoEnum;
import lombok.Data;

@Data
public class FamilyInvitationSendRequest {
    private String receiverEmail;
    private ParentescoEnum parentesco;
}
