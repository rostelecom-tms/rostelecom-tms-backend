package ru.rt.rostelecom_tms.dto.dashboard;

import java.time.LocalDate;

public record DashboardTrendPointDto(
        LocalDate date,
        long totalRuns,
        long passedRuns,
        long failedRuns,
        long brokenRuns,
        long skippedRuns,
        double passRatePercent
) {}
