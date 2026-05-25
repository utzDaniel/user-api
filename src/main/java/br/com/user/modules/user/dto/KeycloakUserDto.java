package br.com.user.modules.user.dto;

public record KeycloakUserDto(
    String id,
    String username,
    String firstName,
    String lastName,
    String email,
    Boolean emailVerified,
    Long familyId,
    String familyName,
    Boolean holder
) {}
