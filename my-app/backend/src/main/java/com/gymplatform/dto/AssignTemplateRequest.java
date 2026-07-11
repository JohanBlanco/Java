package com.gymplatform.dto;

import java.util.List;

public record AssignTemplateRequest(
        Long templateId,
        List<Long> memberIds
) {}
