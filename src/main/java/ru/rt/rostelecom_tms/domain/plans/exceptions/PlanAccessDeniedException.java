package ru.rt.rostelecom_tms.domain.plans.exceptions;

public class PlanAccessDeniedException extends RuntimeException {
    public PlanAccessDeniedException(String message) {
        super(message);
    }
}
