package ru.rt.rostelecom_tms.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.service.users.UserService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserService userService;

    public Optional<User> resolve() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        String email = auth.getName();
        return Optional.of(userService.findByEmail(email));
    }

    public User resolveOrThrow() {
        return resolve().orElseThrow(() ->
                new org.springframework.security.access.AccessDeniedException("Authentication required")
        );
    }
}
