package com.gymplatform.dto;

import com.gymplatform.domain.enums.FormAccessType;
import java.util.List;

public record PublicFormResponse(
        Long id,
        String title,
        String slug,
        String description,
        FormAccessType accessType,
        boolean requiresAuth,
        String organizationName,
        String organizationSlug,
        List<FormFieldDto> fields
) {}
