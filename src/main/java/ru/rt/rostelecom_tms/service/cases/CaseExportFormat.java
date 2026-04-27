package ru.rt.rostelecom_tms.service.cases;

import java.util.Arrays;

public enum CaseExportFormat {
    CSV("csv", "text/csv", "csv");

    private final String value;
    private final String mediaType;
    private final String extension;

    CaseExportFormat(String value, String mediaType, String extension) {
        this.value = value;
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String value() {
        return value;
    }

    public String mediaType() {
        return mediaType;
    }

    public String extension() {
        return extension;
    }

    public static CaseExportFormat from(String raw) {
        if (raw == null || raw.isBlank()) {
            return CSV;
        }

        return Arrays.stream(values())
                .filter(format -> format.value.equalsIgnoreCase(raw.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported export format: " + raw));
    }
}
