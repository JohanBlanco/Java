package com.gymplatform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record FulfillRoutineRequest(
        @NotBlank String name,
        String description,
        String notes,
        Integer daysPerWeek,
        boolean temporary,
        @NotEmpty @Valid List<RoutineDayRequest> days
) {}
