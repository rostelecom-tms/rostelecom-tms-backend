package ru.rt.rostelecom_tms.controller.cases;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.rt.rostelecom_tms.dto.cases.DefectCreateDto;
import ru.rt.rostelecom_tms.dto.cases.DefectResponseDto;
import ru.rt.rostelecom_tms.dto.cases.DefectUpdateDto;
import ru.rt.rostelecom_tms.service.cases.DefectService;
import ru.rt.rostelecom_tms.util.mappers.CaseMapper;

import java.util.List;

@RestController
@RequestMapping("/defects")
@RequiredArgsConstructor
public class DefectController {

    private final DefectService defectService;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<DefectResponseDto> getAll(
            @RequestParam(required = false) Integer caseId
    ) {

        return defectService.findAllByCaseId(caseId)
                .stream()
                .map(CaseMapper::toDto)
                .toList();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public DefectResponseDto create(@RequestBody @Valid DefectCreateDto dto) {
        return CaseMapper.toDto(defectService.create(CaseMapper.toCreateCommand(dto)));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}")
    public void update(@PathVariable Integer id, @RequestBody @Valid DefectUpdateDto dto) {
        defectService.update(id, CaseMapper.toUpdateCommand(dto));
    }
}
