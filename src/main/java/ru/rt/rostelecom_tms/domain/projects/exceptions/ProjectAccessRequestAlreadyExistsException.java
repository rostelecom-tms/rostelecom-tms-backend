package ru.rt.rostelecom_tms.domain.projects.exceptions;

public class ProjectAccessRequestAlreadyExistsException extends RuntimeException {
    public ProjectAccessRequestAlreadyExistsException(String message) {
        super(message);
    }
}
