package ru.rt.rostelecom_tms.domain.projects.exceptions;

public class ProjectAccessDeniedException extends RuntimeException {
    public ProjectAccessDeniedException(String message) {
        super(message);
    }
}
