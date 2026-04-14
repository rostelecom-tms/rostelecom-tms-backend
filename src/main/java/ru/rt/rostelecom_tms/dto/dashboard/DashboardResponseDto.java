package ru.rt.rostelecom_tms.dto.dashboard;

import java.util.List;

public record DashboardResponseDto(
        DashboardTotalsDto totals,
        List<DashboardRecentRunDto> recentRuns,
        List<DashboardRecentPlanDto> recentPlans,
        List<DashboardTrendPointDto> trendLast7Days
) {}
