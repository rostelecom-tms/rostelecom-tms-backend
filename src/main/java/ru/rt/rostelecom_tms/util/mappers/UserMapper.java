package ru.rt.rostelecom_tms.util.mappers;

import org.springframework.stereotype.Component;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.dto.users.UserResponseDto;
import ru.rt.rostelecom_tms.dto.users.UserRoleResponseDto;

@Component
public class UserMapper {

    public static UserResponseDto toDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getCreatedAt(),
                user.isCanCreatePlans(),
                new UserRoleResponseDto(
                        user.getRole().getId(),
                        user.getRole().getName(),
                        user.getRole().getSlug()
                )
        );
    }
}
