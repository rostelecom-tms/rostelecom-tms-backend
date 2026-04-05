package ru.rt.rostelecom_tms.domain.projects.exceptions;

public class ProjectMemberAlreadyExistsException extends RuntimeException {
    public ProjectMemberAlreadyExistsException(String message) {
        super(message);
    }
}
