package ru.rt.rostelecom_tms.domain.runs.exceptions;

public class RunStatusNotFoundException extends RuntimeException {
    public RunStatusNotFoundException(String message) {
        super(message);
    }
}
