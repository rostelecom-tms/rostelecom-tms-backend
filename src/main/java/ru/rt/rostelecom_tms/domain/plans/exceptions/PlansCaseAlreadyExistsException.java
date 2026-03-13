package ru.rt.rostelecom_tms.domain.plans.exceptions;

public class PlansCaseAlreadyExistsException extends RuntimeException {
    public PlansCaseAlreadyExistsException(String message) {
        super(message);
    }
}
