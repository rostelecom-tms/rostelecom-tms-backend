package ru.rt.rostelecom_tms.domain.cases.exceptions;

public class CaseAlreadyInPlanException extends RuntimeException {
    public CaseAlreadyInPlanException(String message) {
        super(message);
    }
}
