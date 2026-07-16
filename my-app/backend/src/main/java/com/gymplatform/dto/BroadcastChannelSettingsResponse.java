package com.gymplatform.dto;

import com.gymplatform.domain.enums.BroadcastChannel;
import java.time.Instant;

public record BroadcastChannelSettingsResponse(
        BroadcastChannel channel,
        String senderPhone,
        boolean enabled,
        boolean whatsappWebSessionConfirmed,
        Instant updatedAt
) {}
