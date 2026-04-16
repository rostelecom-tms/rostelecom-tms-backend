package ru.rt.rostelecom_tms.dto.runs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RunBulkDto (
    @NotNull Integer planId,
    Integer executedBy,
    @Valid @NotEmpty List<ResultDto> results
) {}
