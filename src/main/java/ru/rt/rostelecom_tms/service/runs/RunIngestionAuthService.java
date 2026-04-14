package ru.rt.rostelecom_tms.service.runs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RunIngestionAuthService {

    @Value("${app.runs.ingestion-token:}")
    private String ingestionToken;

    public void assertAuthorized(String token) {
        if (ingestionToken == null || ingestionToken.isBlank()) {
            throw new AccessDeniedException("Runs ingestion token is not configured");
        }

        if (token == null || token.isBlank() || !Objects.equals(ingestionToken, token)) {
            throw new AccessDeniedException("Invalid runs ingestion token");
        }
    }
}
