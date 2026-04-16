package ru.rt.rostelecom_tms.dto.users;

import java.time.Instant;

public record UserResponseDto(
        Integer id,
        String email,
        String username,
        Instant createdAt,
        boolean canCreatePlans,
        UserRoleResponseDto role) {
}
