package ru.rt.rostelecom_tms.dto.plans;

/**
 * DTO for plan responsible {@link ru.rt.rostelecom_tms.domain.users.User}
 */
public record PlanResponsibleUserDto(
        Integer id,
        String username,
        String email
) {}
