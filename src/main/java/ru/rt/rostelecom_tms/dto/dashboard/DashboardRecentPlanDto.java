package ru.rt.rostelecom_tms.dto.dashboard;

import java.time.Instant;

public record DashboardRecentPlanDto(
        Integer id,
        String name,
        Instant createdAt,
        Integer casesCount
) {}
