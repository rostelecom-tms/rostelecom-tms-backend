package ru.rt.rostelecom_tms.dto.projects;

import ru.rt.rostelecom_tms.dto.users.UserResponseDto;

import java.time.Instant;
import java.util.List;

public record ProjectResponseDto(
        Integer id,
        String name,
        String description,
        UserResponseDto owner,
        Instant createdAt,
        List<ProjectMemberResponseDto> members) {
}
