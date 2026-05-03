package br.com.user.modules.profile.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String nomeCompleto;
    private String cpf;
    private String email;
    private String telefone;
}
