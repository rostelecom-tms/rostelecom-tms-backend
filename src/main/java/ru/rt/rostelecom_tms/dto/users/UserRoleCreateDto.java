package ru.rt.rostelecom_tms.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRoleCreateDto(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z_]+") String slug
) {
}
