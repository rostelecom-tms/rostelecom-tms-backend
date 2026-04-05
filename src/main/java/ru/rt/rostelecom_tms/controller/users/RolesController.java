package ru.rt.rostelecom_tms.controller.users;

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
import ru.rt.rostelecom_tms.dto.users.UserRoleCreateDto;
import ru.rt.rostelecom_tms.dto.users.UserRoleResponseDto;
import ru.rt.rostelecom_tms.dto.users.UserRoleUpdateDto;
import ru.rt.rostelecom_tms.service.users.UserRoleService;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolesController {

    private final UserRoleService roleService;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping()
    public List<UserRoleResponseDto> getRoles() {
        return roleService.findAll().stream().map(r -> new UserRoleResponseDto(
                r.getId(), r.getName(), r.getSlug()
        )).toList();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping()
    public void createRole(@RequestBody @Valid UserRoleCreateDto roleDto) {
        roleService.save(new UserRoleService.CreateRoleCommand(
                roleDto.name(), roleDto.slug()
        ));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}")
    public void updateRole(@PathVariable int id, @RequestBody @Valid UserRoleUpdateDto roleDto) {
        roleService.update(id, new UserRoleService.UpdateRoleCommand(
                roleDto.name(), roleDto.slug()
        ));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable int id) {
        roleService.delete(id);
    }
}
