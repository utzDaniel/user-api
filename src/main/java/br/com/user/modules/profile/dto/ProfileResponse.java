package br.com.user.modules.profile.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private String cpf;
    private String email;
    private String nomeCompleto;
    private String telefone;
}
