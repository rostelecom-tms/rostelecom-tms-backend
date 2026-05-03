package ru.rt.rostelecom_tms.dto.cases;

import java.util.List;

public record CaseImportResultDto(
        int imported,
        int created,
        int updated,
        int skipped,
        List<String> errors
) {
}
