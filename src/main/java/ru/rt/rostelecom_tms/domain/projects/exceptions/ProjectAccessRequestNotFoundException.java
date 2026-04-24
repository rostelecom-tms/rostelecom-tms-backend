package ru.rt.rostelecom_tms.domain.projects.exceptions;

public class ProjectAccessRequestNotFoundException extends RuntimeException {
    public ProjectAccessRequestNotFoundException(String message) {
        super(message);
    }
}
