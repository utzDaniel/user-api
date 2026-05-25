package br.com.user.config;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class TimestampUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private TimestampUtils() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada");
    }

    public static String now() {
        return LocalDateTime.now(ZoneOffset.UTC).format(ISO_FORMATTER);
    }
}

