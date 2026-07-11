package com.gymplatform.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RoutineTemplateRequest(
        @NotBlank String name,
        String description,
        String goal,
        List<RoutineExerciseRequest> exercises
) {}
