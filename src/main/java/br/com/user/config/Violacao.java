package br.com.user.config;

import org.springframework.validation.FieldError;

public record Violacao(
        String campo,
        String razao
) {
    public Violacao(FieldError fieldError) {
        this(fieldError.getField(), fieldError.getDefaultMessage());
    }

}

