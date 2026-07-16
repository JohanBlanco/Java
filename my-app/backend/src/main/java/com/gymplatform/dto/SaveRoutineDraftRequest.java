package com.gymplatform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Borrador de rutina para una solicitud (estado En progreso). Los días pueden ir vacíos. */
public record SaveRoutineDraftRequest(
        @NotBlank String name,
        String description,
        String notes,
        Integer daysPerWeek,
        boolean temporary,
        @Valid List<SaveRoutineDraftDayRequest> days
) {}
