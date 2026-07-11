package com.gymplatform.dto;

public record RoutineRequestResponse(
        Long id,
        Long memberId,
        String memberName,
        String description,
        String goals,
        String status,
        Long assignedInstructorId,
        String assignedInstructorName
) {}
