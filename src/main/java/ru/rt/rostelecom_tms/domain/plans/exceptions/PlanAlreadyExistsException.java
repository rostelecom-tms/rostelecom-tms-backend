package ru.rt.rostelecom_tms.domain.plans.exceptions;

public class PlanAlreadyExistsException extends RuntimeException {
    public PlanAlreadyExistsException(String message) {
        super(message);
    }
}
