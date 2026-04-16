package ru.rt.rostelecom_tms.controller;

public record ErrorResponse(
        String message,
        long timestamp
) {
}
