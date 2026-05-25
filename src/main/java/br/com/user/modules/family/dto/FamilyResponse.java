package br.com.user.modules.family.dto;

import java.util.List;

public record FamilyResponse(
    String nome,
    boolean titular,
    List<FamilyMemberResponse> membros
) {}
