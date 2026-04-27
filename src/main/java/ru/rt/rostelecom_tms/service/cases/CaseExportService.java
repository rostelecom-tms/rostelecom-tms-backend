package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.domain.cases.CaseTag;
import ru.rt.rostelecom_tms.domain.users.User;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CaseExportService {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final CaseService caseService;

    public CaseExportResult export(
            CaseExportFormat format,
            Integer groupId,
            Integer planId,
            String title,
            String tag,
            Instant createdFrom,
            Instant createdTo,
            User caller
    ) {
        List<Case> cases = caseService.findAllWithFilters(groupId, planId, title, tag, createdFrom, createdTo, caller)
                .stream()
                .sorted(Comparator.comparing(Case::getId))
                .toList();

        byte[] content = toCsv(cases);
        return new CaseExportResult(filename(format), format.mediaType(), content);
    }

    private byte[] toCsv(List<Case> cases) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("caseId,title,groupId,groupName,groupSlug,tags,description,preconditions,postconditions,createdAt,stepOrder,stepTitle,stepAction,stepExpectedResult\n");

        for (Case testCase : cases) {
            List<CaseStep> steps = sortedSteps(testCase);
            if (steps.isEmpty()) {
                appendCsvRow(csv, testCase, null);
            } else {
                for (CaseStep step : steps) {
                    appendCsvRow(csv, testCase, step);
                }
            }
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendCsvRow(StringBuilder csv, Case testCase, CaseStep step) {
        CaseGroup group = testCase.getGroup();
        csv.append(csvCell(testCase.getId()))
                .append(',')
                .append(csvCell(testCase.getTitle()))
                .append(',')
                .append(csvCell(group == null ? null : group.getId()))
                .append(',')
                .append(csvCell(group == null ? null : group.getName()))
                .append(',')
                .append(csvCell(group == null ? null : group.getSlug()))
                .append(',')
                .append(csvCell(tags(testCase)))
                .append(',')
                .append(csvCell(testCase.getDescription()))
                .append(',')
                .append(csvCell(testCase.getPreconditions()))
                .append(',')
                .append(csvCell(testCase.getPostconditions()))
                .append(',')
                .append(csvCell(testCase.getCreatedAt()))
                .append(',')
                .append(csvCell(step == null ? null : step.getOrder()))
                .append(',')
                .append(csvCell(step == null ? null : step.getTitle()))
                .append(',')
                .append(csvCell(step == null ? null : step.getAction()))
                .append(',')
                .append(csvCell(step == null ? null : step.getExpectedResult()))
                .append('\n');
    }

    private List<CaseStep> sortedSteps(Case testCase) {
        return testCase.getCaseSteps().stream()
                .sorted(Comparator.comparing(CaseStep::getOrder))
                .toList();
    }

    private String groupName(Case testCase) {
        return testCase.getGroup() == null ? "" : testCase.getGroup().getName();
    }

    private String tags(Case testCase) {
        return String.join("; ", tagsList(testCase));
    }

    private List<String> tagsList(Case testCase) {
        return testCase.getTags().stream()
                .map(CaseTag::getName)
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    private String csvCell(Object value) {
        String escaped = nullToEmpty(value).replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String filename(CaseExportFormat format) {
        return "test-cases-%s.%s".formatted(FILE_TIMESTAMP.format(Instant.now()), format.extension());
    }

}
