package com.gymplatform.dto;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        List<String> roles,
        Long organizationId,
        boolean active,
        Instant createdAt,
        MemberProfileResponse profile
) {}
