package ru.rt.rostelecom_tms.domain.cases.exceptions;

public class CaseNotFoundException extends RuntimeException {
    public CaseNotFoundException() {
        super("Case not found");
    }
    public CaseNotFoundException(String message) { super(message); }
}
