package com.gymplatform.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateRoutineRequest(
        @NotBlank String name,
        String description,
        String notes,
        Long memberId,
        Long templateId,
        Long routineRequestId,
        Integer daysPerWeek,
        boolean temporary,
        List<RoutineDayRequest> days,
        List<RoutineExerciseRequest> exercises
) {}
