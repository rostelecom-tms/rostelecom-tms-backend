package ru.rt.rostelecom_tms.dto.users;

import ru.rt.rostelecom_tms.domain.users.User;

/**
 * DTO for {@link User}
 */
public record UserUpdateDto(
        Integer roleId,
        Boolean canCreatePlans
) {
}
