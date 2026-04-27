package ru.rt.rostelecom_tms.dto.users;

public record RegistrationResponseDto(
        Integer id,
        String email,
        String username,
        Integer projectId,
        String projectName
) {}