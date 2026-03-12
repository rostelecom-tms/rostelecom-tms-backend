package ru.rt.rostelecom_tms.domain.cases.exceptions;

public class CaseGroupNotFoundException extends RuntimeException {
    public CaseGroupNotFoundException() {
        super("Case group not found");
    }

    public CaseGroupNotFoundException(String message) {
        super(message);
    }
}
