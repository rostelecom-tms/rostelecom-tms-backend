package ru.rt.rostelecom_tms.util.mappers;

import ru.rt.rostelecom_tms.domain.projects.Project;
import ru.rt.rostelecom_tms.dto.projects.ProjectMemberResponseDto;
import ru.rt.rostelecom_tms.dto.projects.ProjectResponseDto;

import java.util.List;

public class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectResponseDto toDto(Project project) {
        List<ProjectMemberResponseDto> members = project.getMembers().stream()
                .map(m -> new ProjectMemberResponseDto(
                        m.getId(),
                        UserMapper.toDto(m.getUser()),
                        m.getAddedAt()
                ))
                .toList();

        return new ProjectResponseDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                UserMapper.toDto(project.getOwner()),
                project.getCreatedAt(),
                members
        );
    }
}
