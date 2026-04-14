package ru.rt.rostelecom_tms.events;

public record DefectIndexEvent(int defectId, boolean deleted) {
    public static DefectIndexEvent upsert(int defectId) {
        return new DefectIndexEvent(defectId, false);
    }

    public static DefectIndexEvent delete(int defectId) {
        return new DefectIndexEvent(defectId, true);
    }
}
