package com.gymplatform.dto;

import java.time.Instant;

public record FormSubmissionResponse(
        Long id,
        Instant createdAt
) {}
