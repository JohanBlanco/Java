package com.gymplatform.dto;

import java.util.List;

public record RoutineResponse(
        Long id,
        String name,
        String description,
        String notes,
        Long memberId,
        String memberName,
        Long instructorId,
        String instructorName,
        Long templateId,
        boolean temporary,
        List<RoutineExerciseResponse> exercises
) {}
