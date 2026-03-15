package ru.rt.rostelecom_tms.domain.cases.exceptions;

public class DefectNotFoundException extends RuntimeException {
    public DefectNotFoundException(String message) {
        super(message);
    }
}
