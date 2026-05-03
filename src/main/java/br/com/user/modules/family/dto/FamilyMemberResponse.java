package br.com.user.modules.family.dto;

import br.com.user.modules.family.FamilyMemberStatusEnum;
import br.com.user.modules.family.ParentescoEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FamilyMemberResponse {
    private Long id;
    private String nomeCompleto;
    private String email;
    private ParentescoEnum parentesco;
    private FamilyMemberStatusEnum status;
}
