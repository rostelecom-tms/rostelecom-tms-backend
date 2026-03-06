package ru.rt.rostelecom_tms.dto.users;

import jakarta.validation.constraints.Size;

public record UserRoleUpdateDto(
        @Size(max = 100) String name,
        @Size(max = 100) String slug
) {
}
