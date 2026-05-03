package ru.rt.rostelecom_tms.service.cases;

import java.util.Arrays;

public enum CaseImportFormat {
    CSV("csv"),
    PDF("pdf");

    private final String value;

    CaseImportFormat(String value) {
        this.value = value;
    }

    public static CaseImportFormat from(String raw) {
        if (raw == null || raw.isBlank()) {
            return CSV;
        }

        return Arrays.stream(values())
                .filter(format -> format.value.equalsIgnoreCase(raw.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported import format: " + raw));
    }
}
