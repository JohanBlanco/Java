package com.gymplatform.dto;

public record RoutineExerciseResponse(
        Long id,
        String exerciseName,
        Integer sets,
        Integer reps,
        String weight,
        Integer durationSeconds,
        String notes,
        int orderIndex
) {}
