package ru.rt.rostelecom_tms.controller.cases;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.dto.cases.CaseGroupCreateDto;
import ru.rt.rostelecom_tms.dto.cases.CaseGroupResponseDto;
import ru.rt.rostelecom_tms.dto.cases.CaseGroupUpdateDto;
import ru.rt.rostelecom_tms.security.CurrentUserResolver;
import ru.rt.rostelecom_tms.service.cases.CaseGroupService;
import ru.rt.rostelecom_tms.util.mappers.CaseMapper;

import java.util.List;

@RestController
@RequestMapping("/case-groups")
@RequiredArgsConstructor
public class CaseGroupController {

    private final CaseGroupService caseGroupService;
    private final CurrentUserResolver currentUserResolver;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<CaseGroupResponseDto> getAll() {
        User caller = currentUserResolver.resolveOrThrow();
        return caseGroupService.findAll(caller).stream()
                .map(CaseMapper::toDto)
                .toList();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CaseGroupResponseDto create(@RequestBody @Valid CaseGroupCreateDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        return CaseMapper.toDto(
            caseGroupService.create(new CaseGroupService.CreateGroupCommand(dto.name(), dto.slug(), dto.projectId()), caller)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}")
    public void update(@PathVariable int id, @RequestBody @Valid CaseGroupUpdateDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        caseGroupService.update(id, new CaseGroupService.UpdateGroupCommand(dto.name(), dto.slug(), dto.projectId()), caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable int id) {
        User caller = currentUserResolver.resolveOrThrow();
        caseGroupService.delete(id, caller);
    }
}
