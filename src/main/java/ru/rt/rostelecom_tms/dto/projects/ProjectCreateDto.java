package ru.rt.rostelecom_tms.dto.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateDto(
        @NotBlank @Size(max = 255) String name,
        String description) {
}
