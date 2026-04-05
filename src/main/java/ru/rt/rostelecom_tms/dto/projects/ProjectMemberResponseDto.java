package ru.rt.rostelecom_tms.dto.projects;

import ru.rt.rostelecom_tms.dto.users.UserResponseDto;

import java.time.Instant;

public record ProjectMemberResponseDto(Integer id, UserResponseDto user, Instant addedAt) {
}
