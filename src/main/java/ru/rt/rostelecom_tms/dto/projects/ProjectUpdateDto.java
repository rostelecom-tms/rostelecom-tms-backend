package ru.rt.rostelecom_tms.dto.projects;

import jakarta.validation.constraints.Size;

public record ProjectUpdateDto(
        @Size(max = 255) String name,
        String description) {
}
