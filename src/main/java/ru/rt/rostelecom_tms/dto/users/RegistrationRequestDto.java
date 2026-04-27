package ru.rt.rostelecom_tms.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequestDto(
        @NotBlank @Email String email,
        @NotBlank String username,
        @NotBlank @Size(min = 6) String password,
        Integer projectId
) {}