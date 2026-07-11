package com.gymplatform.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateRoutineRequest(
        @NotBlank String name,
        String description,
        String notes,
        Long memberId,
        Long templateId,
        boolean temporary,
        List<RoutineExerciseRequest> exercises
) {}
