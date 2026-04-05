package ru.rt.rostelecom_tms.domain.plans.exceptions;

public class PlanCreationNotAllowedException extends RuntimeException {
    public PlanCreationNotAllowedException(String message) {
        super(message);
    }
}
