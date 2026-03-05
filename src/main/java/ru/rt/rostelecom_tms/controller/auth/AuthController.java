package ru.rt.rostelecom_tms.controller.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import ru.rt.rostelecom_tms.dto.auth.LoginRequest;
import ru.rt.rostelecom_tms.dto.auth.TokenResponse;
import ru.rt.rostelecom_tms.security.jwt.JwtProperties;
import ru.rt.rostelecom_tms.security.jwt.JwtService;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties props;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, JwtProperties props) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.props = props;
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
}