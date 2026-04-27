package ru.rt.rostelecom_tms.service.cases;

public record CaseExportResult(
        String filename,
        String mediaType,
        byte[] content
) {
}
