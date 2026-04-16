package ru.rt.rostelecom_tms.domain.cases.exceptions;

public class CaseGroupAlreadyExistsException extends RuntimeException {
    public CaseGroupAlreadyExistsException(String message) {
        super(message);
    }
}
