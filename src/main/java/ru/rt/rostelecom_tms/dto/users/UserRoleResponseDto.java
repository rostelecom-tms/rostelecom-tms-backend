package ru.rt.rostelecom_tms.dto.users;

import ru.rt.rostelecom_tms.domain.users.UserRole;

/**
 * DTO for {@link UserRole}
 */
public record UserRoleResponseDto(
        Integer id,
        String name,
        String slug
) {}