package br.com.user.modules.family.dto;

import br.com.user.modules.family.FamilyMemberStatusEnum;
import br.com.user.modules.family.ParentescoEnum;
import lombok.Data;

@Data
public class FamilyUpdateMemberRequest {
    private ParentescoEnum parentesco;
    private FamilyMemberStatusEnum status;
}
