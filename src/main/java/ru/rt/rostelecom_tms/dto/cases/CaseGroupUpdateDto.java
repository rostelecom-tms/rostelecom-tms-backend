package ru.rt.rostelecom_tms.dto.cases;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating {@link ru.rt.rostelecom_tms.domain.cases.CaseGroup}
 */
public record CaseGroupUpdateDto(
        @Size(min = 1, max = 255) String name,
        @Size(min = 1, max = 255)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "slug must contain only lowercase letters, digits and hyphens")
        String slug,
        Integer projectId,
        Integer parentId
) {
}
