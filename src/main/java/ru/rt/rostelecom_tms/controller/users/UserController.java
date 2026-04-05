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
import ru.rt.rostelecom_tms.dto.users.UserCreateDto;
import ru.rt.rostelecom_tms.dto.users.UserResponseDto;
import ru.rt.rostelecom_tms.dto.users.UserUpdateDto;
import ru.rt.rostelecom_tms.service.users.UserService;
import ru.rt.rostelecom_tms.util.mappers.UserMapper;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping()
    public List<UserResponseDto> getUsers() {
        return userService.findAll().stream().map(UserMapper::toDto).toList();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping()
    public void createUser(@RequestBody @Valid UserCreateDto userDto) {
        userService.register(
                new UserService.RegisterUserCommand(
                        userDto.email(),
                        userDto.username(),
                        userDto.password(),
                        userDto.role(),
                        Boolean.TRUE.equals(userDto.canCreatePlans())
                )
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}")
    public void updateUser(@PathVariable int id, @RequestBody @Valid UserUpdateDto userDto) {
        userService.update(id, new UserService.UpdateUserCommand(userDto.roleId(), userDto.canCreatePlans()));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable int id) {
        userService.delete(id);
    }
}
