package ru.rt.rostelecom_tms.dto.projects;

import jakarta.validation.constraints.NotNull;

public record ProjectAccessRequestCreateDto(
        @NotNull Integer projectId,
        String comment
) {
}
