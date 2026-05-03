package br.com.user.modules.profile;

import br.com.user.modules.profile.dto.ProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public ProfileResponse toResponse(ProfileEntity entity) {
        return ProfileResponse.builder()
                .cpf(entity.getCpf())
                .email(entity.getEmail())
                .nomeCompleto(entity.getNomeCompleto())
                .telefone(entity.getTelefone())
                .build();
    }
}
