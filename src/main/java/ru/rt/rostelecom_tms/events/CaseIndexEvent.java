package ru.rt.rostelecom_tms.events;

public record CaseIndexEvent(int caseId, boolean deleted) {
    public static CaseIndexEvent upsert(int caseId) {
        return new CaseIndexEvent(caseId, false);
    }

    public static CaseIndexEvent delete(int caseId) {
        return new CaseIndexEvent(caseId, true);
    }
}
