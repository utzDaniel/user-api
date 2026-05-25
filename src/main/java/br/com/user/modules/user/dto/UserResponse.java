package br.com.user.modules.user.dto;

public record UserResponse(
    String nome,
    String sobrenome,
    String email,
    boolean emailVerificado
) {}
