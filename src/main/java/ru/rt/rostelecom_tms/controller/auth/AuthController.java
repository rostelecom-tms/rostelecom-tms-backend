package ru.rt.rostelecom_tms.controller.auth;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import ru.rt.rostelecom_tms.dto.auth.LoginRequest;
import ru.rt.rostelecom_tms.dto.users.RegistrationRequestDto;
import ru.rt.rostelecom_tms.dto.auth.TokenResponse;
import ru.rt.rostelecom_tms.dto.users.UserResponseDto;
import ru.rt.rostelecom_tms.security.jwt.JwtProperties;
import ru.rt.rostelecom_tms.security.jwt.JwtService;
import ru.rt.rostelecom_tms.service.users.UserService;
import ru.rt.rostelecom_tms.util.mappers.UserMapper;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties props;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, JwtProperties props, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.props = props;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        var authToken = new UsernamePasswordAuthenticationToken(req.email(), req.password());
        var auth = authenticationManager.authenticate(authToken);

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String jwt = jwtService.generateToken(req.email(), roles, props.accessTtlSeconds());

        return ResponseEntity.ok(new TokenResponse(jwt));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegistrationRequestDto req) {
        userService.createRegistrationRequest(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public UserResponseDto me(Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        return UserMapper.toDto(user);
    }
}