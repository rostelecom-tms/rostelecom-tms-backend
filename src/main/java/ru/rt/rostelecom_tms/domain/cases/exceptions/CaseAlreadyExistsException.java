package ru.rt.rostelecom_tms.domain.cases.exceptions;

public class CaseAlreadyExistsException extends RuntimeException {
    public CaseAlreadyExistsException(String message) {
        super(message);
    }
}