package br.com.user.modules.family.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FamilyResponse {
    private Long id;
    private String nome;
    private FamilyMemberResponse titular;
    private List<FamilyMemberResponse> membros;
}
