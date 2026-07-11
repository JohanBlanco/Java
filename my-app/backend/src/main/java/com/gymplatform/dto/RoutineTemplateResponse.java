package com.gymplatform.dto;

import java.util.List;

public record RoutineTemplateResponse(
        Long id,
        String name,
        String description,
        String goal,
        Long instructorId,
        List<RoutineExerciseResponse> exercises
) {}
