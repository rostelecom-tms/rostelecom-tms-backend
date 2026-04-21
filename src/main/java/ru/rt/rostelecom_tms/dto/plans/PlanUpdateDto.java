package ru.rt.rostelecom_tms.dto.plans;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Size;
import ru.rt.rostelecom_tms.util.json.FlexibleLocalDateDeserializer;

import java.time.LocalDate;

/**
 * DTO for updating {@link ru.rt.rostelecom_tms.domain.plans.Plan}
 */
public record PlanUpdateDto(
        @Size(min = 1, max = 500) String name,
        String introduction,
        String approach,
        @JsonDeserialize(using = FlexibleLocalDateDeserializer.class) LocalDate startDate,
        @JsonDeserialize(using = FlexibleLocalDateDeserializer.class) LocalDate endDate,
        Integer responsibleUserId,
        Integer projectId
) {}
