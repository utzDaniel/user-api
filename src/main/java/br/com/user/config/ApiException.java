package br.com.user.config;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String error;
    private final List<Violacao> violacoes;

    public ApiException(HttpStatus status, String error, String message, List<Violacao> violacoes) {
        super(message);
        this.status = status;
        this.error = error;
        this.violacoes = violacoes != null ? violacoes : new ArrayList<>();
    }

    public boolean hasViolacoes() {
        return violacoes != null && !violacoes.isEmpty();
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "Não Encontrado", message, new ArrayList<>());
    }

    public static ApiException badRequest(List<Violacao> violacoes) {
        return new ApiException(HttpStatus.BAD_REQUEST, "Requisição Inválida", "Erro de validação", violacoes);
    }

    public static ApiException badRequest(String messag) {
        return new ApiException(HttpStatus.BAD_REQUEST, "Requisição Inválida", messag, new ArrayList<>());
    }

    public static ApiException badGateway(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "Erro no keycloak", message, new ArrayList<>());
    }

    public static ApiException forbiden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "Acesso negado", message, new ArrayList<>());
    }

}

