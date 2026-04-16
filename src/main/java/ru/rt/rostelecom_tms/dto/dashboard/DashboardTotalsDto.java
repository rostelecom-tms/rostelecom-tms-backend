package ru.rt.rostelecom_tms.dto.dashboard;

public record DashboardTotalsDto(
        long totalCases,
        long totalPlans,
        long totalRuns,
        long passedRuns,
        long failedRuns,
        long brokenRuns,
        long skippedRuns,
        double passRatePercent,
        String passRateTrend,
        double passRateLast7DaysPercent,
        double passRatePrevious7DaysPercent
) {}
