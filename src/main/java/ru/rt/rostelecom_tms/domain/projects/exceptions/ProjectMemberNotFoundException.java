package ru.rt.rostelecom_tms.domain.projects.exceptions;

public class ProjectMemberNotFoundException extends RuntimeException {
    public ProjectMemberNotFoundException(String message) {
        super(message);
    }
}
