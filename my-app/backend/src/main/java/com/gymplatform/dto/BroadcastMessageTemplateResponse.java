package com.gymplatform.dto;

import com.gymplatform.domain.enums.BroadcastChannel;
import com.gymplatform.domain.enums.BroadcastTemplatePurpose;
import java.time.Instant;

public record BroadcastMessageTemplateResponse(
        Long id,
        BroadcastChannel channel,
        String name,
        String body,
        BroadcastTemplatePurpose purpose,
        Instant createdAt,
        Instant updatedAt
) {}
