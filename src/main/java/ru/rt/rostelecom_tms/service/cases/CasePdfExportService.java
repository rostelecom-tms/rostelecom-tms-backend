package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.domain.cases.CaseTag;
import ru.rt.rostelecom_tms.domain.users.User;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CasePdfExportService {

    private static final String MEDIA_TYPE = "application/pdf";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final CaseService caseService;

    public CaseExportResult export(int caseId, User caller) {
        Case testCase = caseService.findOne(caseId, caller);

        try (PDDocument document = new PDDocument()) {
            PDFont font = loadUnicodeFont(document);
            PdfWriter writer = new PdfWriter(document, font);

            writer.heading("Тест-кейс #" + testCase.getId());
            writer.field("ID", testCase.getId());
            writer.field("Название", testCase.getTitle());
            writer.field("Группа", testCase.getGroup() == null ? "" : testCase.getGroup().getName());
            writer.field("Теги", tags(testCase));
            writer.field("Создан", testCase.getCreatedAt() == null ? "" : DATE_TIME_FORMATTER.format(testCase.getCreatedAt()));

            writer.section("Описание");
            writer.paragraph(testCase.getDescription(), "Нет описания");

            writer.section("Предусловия");
            writer.paragraph(testCase.getPreconditions(), "Нет предусловий");

            writer.section("Постусловия");
            writer.paragraph(testCase.getPostconditions(), "Нет постусловий");

            writer.section("Шаги");
            List<CaseStep> steps = sortedSteps(testCase);
            if (steps.isEmpty()) {
                writer.paragraph(null, "Шагов нет");
            } else {
                for (int i = 0; i < steps.size(); i++) {
                    CaseStep step = steps.get(i);
                    writer.subsection("Шаг " + (i + 1));
                    writer.field("Название", step.getTitle());
                    writer.field("Действие", step.getAction());
                    writer.field("Ожидаемый результат", step.getExpectedResult());
                }
            }

            writer.close();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return new CaseExportResult(filename(testCase), MEDIA_TYPE, output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate case PDF export", e);
        }
    }

    private PDFont loadUnicodeFont(PDDocument document) throws IOException {
        IOException lastError = null;
        for (String path : List.of(
                "/usr/share/fonts/noto/NotoSans-Regular.ttf",
                "/usr/share/fonts/noto/NotoSansCJK-Regular.ttc",
                "/Library/Fonts/Arial Unicode.ttf",
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf"
        )) {
            File fontFile = new File(path);
            if (fontFile.isFile()) {
                try {
                    return PDType0Font.load(document, fontFile);
                } catch (IOException e) {
                    lastError = e;
                }
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new IllegalStateException("No Unicode font found for PDF export");
    }

    private List<CaseStep> sortedSteps(Case testCase) {
        return testCase.getCaseSteps().stream()
                .sorted(Comparator.comparing(CaseStep::getOrder))
                .toList();
    }

    private String tags(Case testCase) {
        return String.join("; ", testCase.getTags().stream()
                .map(CaseTag::getName)
                .sorted(String::compareToIgnoreCase)
                .toList());
    }

    private String filename(Case testCase) {
        String title = testCase.getTitle() == null ? "case" : testCase.getTitle();
        String slug = title.toLowerCase()
                .replaceAll("[^a-z0-9а-яё]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            slug = "case";
        }
        return "test-case-%s-%s.pdf".formatted(testCase.getId(), slug);
    }

    private static class PdfWriter {
        private static final float MARGIN = 48;
        private static final float FONT_SIZE = 11;
        private static final float HEADING_SIZE = 20;
        private static final float SECTION_SIZE = 15;
        private static final float LINE_HEIGHT = 15;

        private final PDDocument document;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private PdfWriter(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        private void heading(String text) throws IOException {
            write(text, HEADING_SIZE, 22);
            gap(8);
        }

        private void section(String text) throws IOException {
            gap(8);
            write(text, SECTION_SIZE, 19);
            gap(2);
        }

        private void subsection(String text) throws IOException {
            gap(6);
            write(text, 12, 16);
        }

        private void field(String label, Object value) throws IOException {
            String text = label + ": " + normalize(value, "");
            for (String line : wrap(text, FONT_SIZE)) {
                write(line, FONT_SIZE, LINE_HEIGHT);
            }
        }

        private void paragraph(String value, String emptyText) throws IOException {
            String text = normalize(value, emptyText);
            for (String part : text.split("\\R", -1)) {
                if (part.isBlank()) {
                    gap(LINE_HEIGHT);
                    continue;
                }
                for (String line : wrap(part, FONT_SIZE)) {
                    write(line, FONT_SIZE, LINE_HEIGHT);
                }
            }
        }

        private void write(String text, float fontSize, float lineHeight) throws IOException {
            ensureSpace(lineHeight);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText(text);
            contentStream.endText();
            y -= lineHeight;
        }

        private List<String> wrap(String text, float fontSize) throws IOException {
            float maxWidth = page.getMediaBox().getWidth() - MARGIN * 2;
            java.util.ArrayList<String> lines = new java.util.ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.getStringWidth(candidate) / 1000 * fontSize <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                }
                line.setLength(0);
                line.append(word);
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private void gap(float size) throws IOException {
            ensureSpace(size);
            y -= size;
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required >= MARGIN) {
                return;
            }
            newPage();
        }

        private void newPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }

        private String normalize(Object value, String emptyText) {
            if (value == null || String.valueOf(value).isBlank()) {
                return emptyText;
            }
            return String.valueOf(value).replace("\t", " ");
        }
    }
}
