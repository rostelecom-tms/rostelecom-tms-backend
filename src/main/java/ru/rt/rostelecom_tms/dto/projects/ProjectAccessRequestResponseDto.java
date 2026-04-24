package ru.rt.rostelecom_tms.dto.projects;

import ru.rt.rostelecom_tms.dto.users.UserResponseDto;

import java.time.Instant;

public record ProjectAccessRequestResponseDto(
        Integer id,
        Integer projectId,
        String projectName,
        UserResponseDto requester,
        UserResponseDto approver,
        String destination,
        String status,
        String comment,
        String decisionComment,
        Instant createdAt,
        Instant processedAt,
        UserResponseDto processedBy
) {
}
