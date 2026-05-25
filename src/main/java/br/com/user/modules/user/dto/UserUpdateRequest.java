package br.com.user.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    String nome,

    @NotBlank(message = "Sobrenome é obrigatório")
    @Size(max = 255, message = "Sobrenome deve ter no máximo 255 caracteres")
    String sobrenome,

    @NotBlank(message = "Email é obrigatório")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    @Email(message = "Email deve ser válido")
    String email
) {}
