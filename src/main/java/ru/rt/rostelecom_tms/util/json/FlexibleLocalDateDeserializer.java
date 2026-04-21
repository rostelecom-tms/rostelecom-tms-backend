package ru.rt.rostelecom_tms.util.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Accepts either plain local date (yyyy-MM-dd) or ISO date-time and normalizes to LocalDate.
 */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String raw = parser.getValueAsString();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // Continue with date-time fallbacks.
        }

        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Continue with date-time fallbacks.
        }

        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Continue with instant fallback.
        }

        try {
            return Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Final error below.
        }

        throw InvalidFormatException.from(
                parser,
                "Invalid date value. Use yyyy-MM-dd or ISO-8601 date-time",
                value,
                LocalDate.class
        );
    }
}
