package ru.rt.rostelecom_tms.domain.cases.exceptions;

public class CaseGroupNotCreatedException extends RuntimeException {
    public CaseGroupNotCreatedException(String message) {
        super(message);
    }
}
