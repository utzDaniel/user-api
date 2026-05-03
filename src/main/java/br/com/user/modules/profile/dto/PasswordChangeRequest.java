package br.com.user.modules.profile.dto;

import lombok.Data;

@Data
public class PasswordChangeRequest {
    private String senhaAtual;
    private String novaSenha;
    private String confirmarNovaSenha;
}
