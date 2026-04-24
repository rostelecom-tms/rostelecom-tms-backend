package ru.rt.rostelecom_tms.util.mappers;

import ru.rt.rostelecom_tms.domain.projects.ProjectAccessRequest;
import ru.rt.rostelecom_tms.dto.projects.ProjectAccessRequestResponseDto;

public class ProjectAccessRequestMapper {

    private ProjectAccessRequestMapper() {
    }

    public static ProjectAccessRequestResponseDto toDto(ProjectAccessRequest request) {
        return new ProjectAccessRequestResponseDto(
                request.getId(),
                request.getProject().getId(),
                request.getProject().getName(),
                UserMapper.toDto(request.getRequesterUser()),
                request.getApproverUser() == null ? null : UserMapper.toDto(request.getApproverUser()),
                request.getDestination().name(),
                request.getStatus().name(),
                request.getComment(),
                request.getDecisionComment(),
                request.getCreatedAt(),
                request.getProcessedAt(),
                request.getProcessedByUser() == null ? null : UserMapper.toDto(request.getProcessedByUser())
        );
    }
}
