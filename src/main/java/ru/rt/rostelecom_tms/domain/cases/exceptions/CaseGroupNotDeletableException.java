package ru.rt.rostelecom_tms.domain.cases.exceptions;

public class CaseGroupNotDeletableException extends RuntimeException {
    public CaseGroupNotDeletableException(String message) {
        super(message);
    }
}
