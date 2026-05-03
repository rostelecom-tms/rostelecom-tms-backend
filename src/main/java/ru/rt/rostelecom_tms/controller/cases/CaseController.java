package ru.rt.rostelecom_tms.controller.cases;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.config.cache.CacheNames;
import ru.rt.rostelecom_tms.dto.cases.CaseCreateDto;
import ru.rt.rostelecom_tms.dto.cases.CaseImportResultDto;
import ru.rt.rostelecom_tms.dto.cases.CaseResponseDto;
import ru.rt.rostelecom_tms.dto.cases.CaseSimpleResponseDto;
import ru.rt.rostelecom_tms.dto.cases.CaseUpdateDto;
import ru.rt.rostelecom_tms.dto.common.PageResponseDto;
import ru.rt.rostelecom_tms.security.CurrentUserResolver;
import ru.rt.rostelecom_tms.service.cases.CaseExportFormat;
import ru.rt.rostelecom_tms.service.cases.CaseExportService;
import ru.rt.rostelecom_tms.service.cases.CaseExportResult;
import ru.rt.rostelecom_tms.service.cases.CaseImportFormat;
import ru.rt.rostelecom_tms.service.cases.CaseImportService;
import ru.rt.rostelecom_tms.service.cases.CasePdfExportService;
import ru.rt.rostelecom_tms.service.cases.CaseService;
import ru.rt.rostelecom_tms.util.PaginationUtils;
import ru.rt.rostelecom_tms.util.mappers.CaseMapper;

import java.time.Instant;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    private final CaseExportService caseExportService;
    private final CasePdfExportService casePdfExportService;
    private final CaseImportService caseImportService;
    private final CurrentUserResolver currentUserResolver;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Cacheable(
            value = CacheNames.CASES_PAGE,
            key = "T(java.lang.String).format('%s|%s|%s|%s|%s|%s|%s|%s|%s', #authentication?.name, #groupId, #planId, #title, #tag, #createdFrom, #createdTo, #page, #size)"
    )
    public PageResponseDto<CaseSimpleResponseDto> getAll(
            Authentication authentication,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer planId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        User caller = currentUserResolver.resolveOrThrow();
        return PaginationUtils.paginate(caseService.findAllWithFilters(
                        groupId,
                        planId,
                        title,
                        tag,
                        createdFrom,
                        createdTo,
                        caller
                ).stream()
                .map(CaseMapper::toSimpleDto)
                .toList(), page, size);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer planId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo
    ) {
        User caller = currentUserResolver.resolveOrThrow();
        CaseExportResult export = caseExportService.export(
                CaseExportFormat.from(format),
                groupId,
                planId,
                title,
                tag,
                createdFrom,
                createdTo,
                caller
        );

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(export.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(export.content());
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable int id) {
        User caller = currentUserResolver.resolveOrThrow();
        CaseExportResult export = casePdfExportService.export(id, caller);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(export.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(export.content());
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CaseImportResultDto importCases(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Integer groupId
    ) {
        User caller = currentUserResolver.resolveOrThrow();
        return caseImportService.importFile(CaseImportFormat.from(format), file, groupId, caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public CaseResponseDto getOne(@PathVariable int id) {
        User caller = currentUserResolver.resolveOrThrow();
        return CaseMapper.toDto(caseService.findOne(id, caller));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CaseResponseDto create(@RequestBody @Valid CaseCreateDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        return CaseMapper.toDto(caseService.create(CaseMapper.toCreateCommand(dto), caller));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}")
    public void update(@PathVariable int id, @RequestBody @Valid CaseUpdateDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        caseService.update(id, CaseMapper.toUpdateCommand(dto), caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable int id) {
        User caller = currentUserResolver.resolveOrThrow();
        caseService.delete(id, caller);
    }
}
