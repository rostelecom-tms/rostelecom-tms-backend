package ru.rt.rostelecom_tms.controller.users;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rt.rostelecom_tms.dto.users.UserRoleResponseDto;
import ru.rt.rostelecom_tms.service.users.UserRoleService;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolesController {

    private final UserRoleService roleService;

    @GetMapping
    public List<UserRoleResponseDto> getRoles() {
        return roleService.findAll().stream()
                .map(r -> new UserRoleResponseDto(r.getId(), r.getName(), r.getSlug()))
                .toList();
    }
}
