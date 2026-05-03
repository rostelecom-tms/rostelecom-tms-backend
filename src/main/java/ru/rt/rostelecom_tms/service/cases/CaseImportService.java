package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.dto.cases.CaseImportResultDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static ru.rt.rostelecom_tms.service.cases.CaseStepService.StepCommand;

@Service
@RequiredArgsConstructor
public class CaseImportService {

    private static final Set<String> PDF_FIELD_PREFIXES = Set.of(
            "ID:",
            "Название:",
            "Группа:",
            "Теги:",
            "Создан:",
            "Действие:",
            "Ожидаемый результат:"
    );
    private static final Set<String> PDF_SECTION_HEADINGS = Set.of(
            "Описание",
            "Предусловия",
            "Постусловия",
            "Шаги"
    );

    private final CaseService caseService;

    public CaseImportResultDto importFile(
            CaseImportFormat format,
            MultipartFile file,
            Integer groupId,
            User caller
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Import file is empty");
        }

        return switch (format) {
            case CSV -> importCsv(file, groupId, caller);
            case PDF -> importPdf(file, groupId, caller);
        };
    }

    private CaseImportResultDto importCsv(MultipartFile file, Integer overrideGroupId, User caller) {
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV import file", e);
        }

        List<List<String>> rows = parseCsv(stripBom(content));
        if (rows.isEmpty()) {
            return new CaseImportResultDto(0, 0, 0, 0, List.of());
        }

        Map<String, Integer> headers = headers(rows.get(0));
        if (!headers.containsKey("title")) {
            throw new IllegalArgumentException("CSV must contain title column");
        }

        Map<String, CaseDraft> drafts = new LinkedHashMap<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(this::isBlank)) {
                continue;
            }

            Integer sourceCaseId = parseInteger(value(row, headers, "caseId"), null);
            Integer rowGroupId = parseInteger(value(row, headers, "groupId"), null);
            Integer groupId = overrideGroupId != null ? overrideGroupId : rowGroupId;
            String title = value(row, headers, "title");
            String key = sourceCaseId != null
                    ? "id:" + sourceCaseId
                    : "case:" + nullToEmpty(title).trim().toLowerCase(Locale.ROOT) + ":" + groupId;

            int rowNumber = i + 1;
            CaseDraft draft = drafts.computeIfAbsent(key, ignored -> new CaseDraft(rowNumber, sourceCaseId));
            draft.groupId = groupId;
            draft.title = firstPresent(draft.title, title);
            draft.description = firstPresent(draft.description, value(row, headers, "description"));
            draft.preconditions = firstPresent(draft.preconditions, value(row, headers, "preconditions"));
            draft.postconditions = firstPresent(draft.postconditions, value(row, headers, "postconditions"));
            splitTags(value(row, headers, "tags")).forEach(draft.tags::add);

            String stepTitle = value(row, headers, "stepTitle");
            String stepAction = value(row, headers, "stepAction");
            String stepExpectedResult = value(row, headers, "stepExpectedResult");
            if (!isBlank(stepTitle) || !isBlank(stepAction) || !isBlank(stepExpectedResult)) {
                draft.steps.add(new StepDraft(
                        parseInteger(value(row, headers, "stepOrder"), null),
                        stepTitle,
                        stepAction,
                        stepExpectedResult
                ));
            }
        }

        return importDrafts(drafts.values().stream().toList(), caller);
    }

    private CaseImportResultDto importPdf(MultipartFile file, Integer groupId, User caller) {
        if (groupId == null) {
            throw new IllegalArgumentException("groupId is required for PDF import");
        }

        String text;
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            text = new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read PDF import file", e);
        }

        List<String> lines = text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        int descriptionIndex = indexOf(lines, "Описание", 0);
        int preconditionsIndex = indexOf(lines, "Предусловия", 0);
        int postconditionsIndex = indexOf(lines, "Постусловия", 0);
        int stepsIndex = indexOf(lines, "Шаги", 0);
        int fieldsEnd = firstNonNegative(descriptionIndex, preconditionsIndex, postconditionsIndex, stepsIndex, lines.size());

        CaseDraft draft = new CaseDraft(1, parseInteger(readPdfField(lines, 0, fieldsEnd, "ID"), null));
        draft.groupId = groupId;
        draft.title = readPdfField(lines, 0, fieldsEnd, "Название");
        draft.tags.addAll(splitTags(readPdfField(lines, 0, fieldsEnd, "Теги")));
        draft.description = readPdfSection(lines, descriptionIndex, firstNonNegative(preconditionsIndex, postconditionsIndex, stepsIndex, lines.size()));
        draft.preconditions = readPdfSection(lines, preconditionsIndex, firstNonNegative(postconditionsIndex, stepsIndex, lines.size()));
        draft.postconditions = readPdfSection(lines, postconditionsIndex, firstNonNegative(stepsIndex, lines.size()));
        draft.steps.addAll(readPdfSteps(lines, stepsIndex));

        return importDrafts(List.of(draft), caller);
    }

    private CaseImportResultDto importDrafts(List<CaseDraft> drafts, User caller) {
        ImportStats stats = new ImportStats();
        for (CaseDraft draft : drafts) {
            try {
                validateDraft(draft);
                List<StepCommand> steps = toStepCommands(draft.steps);
                if (draft.sourceCaseId != null && updateExisting(draft, steps, caller)) {
                    stats.updated++;
                } else {
                    caseService.create(new CaseService.CreateCaseCommand(
                            trimToNull(draft.title),
                            draft.groupId,
                            trimToNull(draft.description),
                            trimToNull(draft.preconditions),
                            trimToNull(draft.postconditions),
                            new ArrayList<>(draft.tags),
                            steps
                    ), caller);
                    stats.created++;
                }
            } catch (RuntimeException e) {
                stats.skipped++;
                stats.errors.add("Case from row " + draft.rowNumber + ": " + e.getMessage());
            }
        }

        return stats.toDto();
    }

    private boolean updateExisting(CaseDraft draft, List<StepCommand> steps, User caller) {
        try {
            caseService.findOne(draft.sourceCaseId, caller);
        } catch (CaseNotFoundException e) {
            return false;
        }

        caseService.update(draft.sourceCaseId, new CaseService.UpdateCaseCommand(
                trimToNull(draft.title),
                draft.groupId,
                trimToNull(draft.description),
                trimToNull(draft.preconditions),
                trimToNull(draft.postconditions),
                new ArrayList<>(draft.tags),
                steps
        ), caller);
        return true;
    }

    private void validateDraft(CaseDraft draft) {
        if (isBlank(draft.title)) {
            throw new IllegalArgumentException("title is required");
        }
        if (draft.groupId == null) {
            throw new IllegalArgumentException("groupId is required");
        }
    }

    private List<StepCommand> toStepCommands(List<StepDraft> drafts) {
        List<StepDraft> sorted = drafts.stream()
                .sorted(Comparator.comparing(step -> step.order == null ? Integer.MAX_VALUE : step.order))
                .toList();
        List<StepCommand> result = new ArrayList<>();
        int nextOrder = 1;
        for (StepDraft draft : sorted) {
            int order = draft.order == null ? nextOrder : draft.order;
            if (isBlank(draft.action)) {
                throw new IllegalArgumentException("step action is required");
            }
            result.add(new StepCommand(
                    order,
                    trimToNull(draft.title),
                    trimToNull(draft.action),
                    trimToNull(draft.expectedResult)
            ));
            nextOrder = Math.max(nextOrder, order + 1);
        }
        return result;
    }

    private List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(ch);
                }
                continue;
            }

            if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (ch == '\n') {
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
            } else if (ch != '\r') {
                cell.append(ch);
            }
        }

        row.add(cell.toString());
        if (!(row.size() == 1 && row.get(0).isBlank())) {
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Integer> headers(List<String> headerRow) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            result.put(normalizeHeader(headerRow.get(i)), i);
        }
        return result;
    }

    private String value(List<String> row, Map<String, Integer> headers, String name) {
        Integer index = headers.get(normalizeHeader(name));
        if (index == null || index >= row.size()) {
            return null;
        }
        return row.get(index);
    }

    private String normalizeHeader(String header) {
        return nullToEmpty(header).trim().toLowerCase(Locale.ROOT);
    }

    private List<StepDraft> readPdfSteps(List<String> lines, int stepsIndex) {
        if (stepsIndex < 0) {
            return List.of();
        }

        List<StepDraft> steps = new ArrayList<>();
        int cursor = stepsIndex + 1;
        while (cursor < lines.size()) {
            String line = lines.get(cursor);
            if (!line.matches("Шаг\\s+\\d+.*")) {
                cursor++;
                continue;
            }

            int next = cursor + 1;
            while (next < lines.size() && !lines.get(next).matches("Шаг\\s+\\d+.*")) {
                next++;
            }

            steps.add(new StepDraft(
                    steps.size() + 1,
                    readPdfField(lines, cursor + 1, next, "Название"),
                    readPdfField(lines, cursor + 1, next, "Действие"),
                    readPdfField(lines, cursor + 1, next, "Ожидаемый результат")
            ));
            cursor = next;
        }
        return steps;
    }

    private String readPdfField(List<String> lines, int from, int to, String label) {
        String prefix = label + ":";
        for (int i = Math.max(0, from); i < Math.min(to, lines.size()); i++) {
            String line = lines.get(i);
            if (!line.startsWith(prefix)) {
                continue;
            }

            List<String> parts = new ArrayList<>();
            parts.add(line.substring(prefix.length()).trim());
            for (int j = i + 1; j < Math.min(to, lines.size()); j++) {
                String next = lines.get(j);
                if (isPdfBoundary(next)) {
                    break;
                }
                parts.add(next);
            }
            return trimToNull(String.join("\n", parts));
        }
        return null;
    }

    private String readPdfSection(List<String> lines, int start, int end) {
        if (start < 0) {
            return null;
        }

        List<String> section = new ArrayList<>();
        for (int i = start + 1; i < Math.min(end, lines.size()); i++) {
            section.add(lines.get(i));
        }
        return trimToNull(String.join("\n", section));
    }

    private boolean isPdfBoundary(String line) {
        return PDF_FIELD_PREFIXES.stream().anyMatch(line::startsWith)
                || PDF_SECTION_HEADINGS.contains(line)
                || line.matches("Шаг\\s+\\d+.*");
    }

    private int indexOf(List<String> lines, String value, int from) {
        for (int i = Math.max(0, from); i < lines.size(); i++) {
            if (value.equals(lines.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int firstNonNegative(int... values) {
        for (int value : values) {
            if (value >= 0) {
                return value;
            }
        }
        return -1;
    }

    private List<String> splitTags(String value) {
        if (isBlank(value)) {
            return List.of();
        }

        List<String> tags = new ArrayList<>();
        for (String tag : value.split(";")) {
            String trimmed = trimToNull(tag);
            if (trimmed != null) {
                tags.add(trimmed);
            }
        }
        return tags;
    }

    private Integer parseInteger(String value, Integer fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String stripBom(String value) {
        if (value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private String firstPresent(String current, String next) {
        return isBlank(current) ? next : current;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class CaseDraft {
        private final int rowNumber;
        private final Integer sourceCaseId;
        private Integer groupId;
        private String title;
        private String description;
        private String preconditions;
        private String postconditions;
        private final LinkedHashSet<String> tags = new LinkedHashSet<>();
        private final List<StepDraft> steps = new ArrayList<>();

        private CaseDraft(int rowNumber, Integer sourceCaseId) {
            this.rowNumber = rowNumber;
            this.sourceCaseId = sourceCaseId;
        }
    }

    private record StepDraft(
            Integer order,
            String title,
            String action,
            String expectedResult
    ) {
    }

    private static final class ImportStats {
        private int created;
        private int updated;
        private int skipped;
        private final List<String> errors = new ArrayList<>();

        private CaseImportResultDto toDto() {
            return new CaseImportResultDto(created + updated, created, updated, skipped, errors);
        }
    }
}
