package ru.rt.rostelecom_tms.dto.runs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RunBulkDto (
    @NotNull Integer planId,
    @NotNull Integer executedBy,
    @Valid List<ResultDto> results
) {}
