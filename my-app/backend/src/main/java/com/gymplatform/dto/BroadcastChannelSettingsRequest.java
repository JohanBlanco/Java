package com.gymplatform.dto;

import com.gymplatform.domain.enums.BroadcastChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BroadcastChannelSettingsRequest(
        @Size(max = 32) String senderPhone,
        @NotNull Boolean enabled,
        Boolean whatsappWebSessionConfirmed
) {}
