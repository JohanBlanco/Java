package com.gymplatform.dto;

import jakarta.validation.constraints.NotBlank;

public record RoutineExerciseRequest(
        @NotBlank String exerciseName,
        Integer sets,
        Integer reps,
        String weight,
        Integer durationSeconds,
        String notes,
        int orderIndex
) {}
