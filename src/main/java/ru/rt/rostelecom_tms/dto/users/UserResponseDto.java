package ru.rt.rostelecom_tms.dto.users;

import ru.rt.rostelecom_tms.domain.users.User;

import java.time.Instant;

/**
 * DTO for {@link User}
 */
public record UserResponseDto(
        Integer id,
        String email,
        String username,
        Instant createdAt,
        UserRoleDto role) {
}