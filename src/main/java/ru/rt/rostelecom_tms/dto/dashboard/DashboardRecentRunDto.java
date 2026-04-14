package ru.rt.rostelecom_tms.dto.dashboard;

import java.time.Instant;

public record DashboardRecentRunDto(
        Integer id,
        Integer caseId,
        String caseTitle,
        Integer planId,
        String planName,
        String statusSlug,
        String statusName,
        Integer executedBy,
        String executedByUsername,
        String executedByEmail,
        Instant executedAt
) {}
