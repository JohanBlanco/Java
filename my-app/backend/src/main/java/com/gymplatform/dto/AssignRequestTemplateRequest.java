package com.gymplatform.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequestTemplateRequest(
        @NotNull Long templateId
) {}
