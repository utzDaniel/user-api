package br.com.user.modules.family.dto;

public record FamilyMemberResponse(
    String username,
    String nome,
    String sobrenome,
    String email,
    Boolean emailVerified,
    boolean ehDeletavel
) {}
