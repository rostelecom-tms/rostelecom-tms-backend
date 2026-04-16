package ru.rt.rostelecom_tms.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.config.cache.CacheNames;
import ru.rt.rostelecom_tms.domain.runs.Run;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.dto.dashboard.DashboardRecentPlanDto;
import ru.rt.rostelecom_tms.dto.dashboard.DashboardRecentRunDto;
import ru.rt.rostelecom_tms.dto.dashboard.DashboardResponseDto;
import ru.rt.rostelecom_tms.dto.dashboard.DashboardTotalsDto;
import ru.rt.rostelecom_tms.dto.dashboard.DashboardTrendPointDto;
import ru.rt.rostelecom_tms.service.cases.CaseService;
import ru.rt.rostelecom_tms.service.plans.PlanService;
import ru.rt.rostelecom_tms.service.runs.RunService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final CaseService caseService;
    private final PlanService planService;
    private final RunService runService;

    @Cacheable(value = CacheNames.DASHBOARD, key = "#caller.id")
    public DashboardResponseDto buildDashboard(User caller) {
        List<Run> runs = runService.findAllWithProbableFilters(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                caller
        );

        long totalCases = caseService.findAll(caller).size();
        long totalPlans = planService.findAll(caller).size();
        long totalRuns = runs.size();

        long passedRuns = countByStatus(runs, "passed");
        long failedRuns = countByStatus(runs, "failed");
        long brokenRuns = countByStatus(runs, "broken");
        long skippedRuns = countByStatus(runs, "skipped");

        double passRatePercent = computeRate(passedRuns, totalRuns);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant last7Start = today.minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant previous7Start = today.minusDays(13).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant previous7End = today.minusDays(7).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

        List<Run> last7Runs = runs.stream()
                .filter(run -> !run.getExecutedAt().isBefore(last7Start))
                .toList();
        List<Run> previous7Runs = runs.stream()
                .filter(run -> !run.getExecutedAt().isBefore(previous7Start))
                .filter(run -> !run.getExecutedAt().isAfter(previous7End))
                .toList();

        double passRateLast7 = computeRate(countByStatus(last7Runs, "passed"), last7Runs.size());
        double passRatePrevious7 = computeRate(countByStatus(previous7Runs, "passed"), previous7Runs.size());
        String trend = resolveTrend(passRateLast7, passRatePrevious7);

        DashboardTotalsDto totals = new DashboardTotalsDto(
                totalCases,
                totalPlans,
                totalRuns,
                passedRuns,
                failedRuns,
                brokenRuns,
                skippedRuns,
                passRatePercent,
                trend,
                passRateLast7,
                passRatePrevious7
        );

        List<DashboardRecentRunDto> recentRuns = runs.stream()
                .limit(10)
                .map(run -> new DashboardRecentRunDto(
                        run.getId(),
                        run.getCaseField().getId(),
                        run.getCaseField().getTitle(),
                        run.getPlan().getId(),
                        run.getPlan().getName(),
                        run.getStatus().getSlug(),
                        run.getStatus().getName(),
                        run.getExecutedBy() == null ? null : run.getExecutedBy().getId(),
                        run.getExecutedBy() == null ? null : run.getExecutedBy().getUsername(),
                        run.getExecutedBy() == null ? null : run.getExecutedBy().getEmail(),
                        run.getExecutedAt()
                ))
                .toList();

        List<DashboardRecentPlanDto> recentPlans = planService.findAll(caller).stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .limit(5)
                .map(plan -> new DashboardRecentPlanDto(
                        plan.getId(),
                        plan.getName(),
                        plan.getCreatedAt(),
                        plan.getCases() == null ? 0 : plan.getCases().size()
                ))
                .toList();

        List<DashboardTrendPointDto> trendLast7Days = buildTrend(runs, today);

        return new DashboardResponseDto(
                totals,
                recentRuns,
                recentPlans,
                trendLast7Days
        );
    }

    private long countByStatus(List<Run> runs, String statusSlug) {
        return runs.stream()
                .filter(run -> statusSlug.equals(run.getStatus().getSlug()))
                .count();
    }

    private double computeRate(long passed, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((passed * 10000.0) / total) / 100.0;
    }

    private String resolveTrend(double last7, double previous7) {
        if (last7 > previous7) {
            return "UP";
        }
        if (last7 < previous7) {
            return "DOWN";
        }
        return "STABLE";
    }

    private List<DashboardTrendPointDto> buildTrend(List<Run> runs, LocalDate today) {
        List<DashboardTrendPointDto> points = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

            List<Run> dayRuns = runs.stream()
                    .filter(run -> !run.getExecutedAt().isBefore(start))
                    .filter(run -> !run.getExecutedAt().isAfter(end))
                    .toList();

            long totalRuns = dayRuns.size();
            long passedRuns = countByStatus(dayRuns, "passed");
            long failedRuns = countByStatus(dayRuns, "failed");
            long brokenRuns = countByStatus(dayRuns, "broken");
            long skippedRuns = countByStatus(dayRuns, "skipped");

            points.add(new DashboardTrendPointDto(
                    date,
                    totalRuns,
                    passedRuns,
                    failedRuns,
                    brokenRuns,
                    skippedRuns,
                    computeRate(passedRuns, totalRuns)
            ));
        }

        return points;
    }
}
