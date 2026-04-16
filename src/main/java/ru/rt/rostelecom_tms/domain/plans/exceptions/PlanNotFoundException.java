package ru.rt.rostelecom_tms.domain.plans.exceptions;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException() {
        super("Plan not found");
    }
    public PlanNotFoundException(String message) {
        super(message);
    }
}
