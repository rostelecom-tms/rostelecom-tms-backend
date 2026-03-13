package ru.rt.rostelecom_tms.domain.plans.exceptions;

public class PlansCaseNotFoundException extends RuntimeException {
    public PlansCaseNotFoundException() {
        super("Plans-case entry not found");
    }
    public PlansCaseNotFoundException(String message) {
        super(message);
    }
}
