package com.gymplatform.dto;

import com.gymplatform.domain.enums.BroadcastTemplatePurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastMessageTemplateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 4096) String body,
        BroadcastTemplatePurpose purpose
) {}
