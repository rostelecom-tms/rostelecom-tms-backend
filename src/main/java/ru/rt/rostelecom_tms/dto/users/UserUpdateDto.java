package ru.rt.rostelecom_tms.dto.users;

import jakarta.validation.constraints.NotNull;
import ru.rt.rostelecom_tms.domain.users.User;

/**
 * DTO for {@link User}
 */
public record UserUpdateDto(
        @NotNull Integer roleId
) {
}