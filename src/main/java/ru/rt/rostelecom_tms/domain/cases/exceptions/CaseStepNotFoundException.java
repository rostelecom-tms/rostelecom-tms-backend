package ru.rt.rostelecom_tms.domain.cases.exceptions;

public class CaseStepNotFoundException extends RuntimeException {
    public CaseStepNotFoundException(String message) {
        super(message);
    }
}
