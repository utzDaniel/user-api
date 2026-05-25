package br.com.user.modules.user.dto;

public record PasswordChangeRequest(
    String senhaAtual,
    String novaSenha,
    String confirmarNovaSenha
) {}
