package ru.rt.rostelecom_tms.controller.users;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rt.rostelecom_tms.controller.ErrorResponse;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserNotCreatedException;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserNotFoundException;
import ru.rt.rostelecom_tms.dto.users.UserCreateDto;
import ru.rt.rostelecom_tms.dto.users.UserResponseDto;
import ru.rt.rostelecom_tms.dto.users.UserRoleDto;
import ru.rt.rostelecom_tms.service.users.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping()
    public List<UserResponseDto> getUsers() {
        return userService.findAll().stream().map(u -> new UserResponseDto(
                u.getId(),
                u.getEmail(),
                u.getUsername(),
                u.getCreatedAt(),
                new UserRoleDto(u.getRole().getId(), u.getRole().getName(), u.getRole().getSlug())
        )).toList();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<HttpStatus> create(@RequestBody @Valid UserCreateDto userDto) {
        userService.register(
                new UserService.RegisterUserCommand(
                        userDto.email(), userDto.username(), userDto.password()
                )
        );
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(UserNotFoundException e) {
        ErrorResponse response = new ErrorResponse(
                e.getMessage(),
                System.currentTimeMillis()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(UserNotCreatedException e) {
        ErrorResponse response = new ErrorResponse(
                e.getMessage(),
                System.currentTimeMillis()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
