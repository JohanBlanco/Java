package com.gymplatform.controller;

import com.gymplatform.dto.OrganizationResponse;
import com.gymplatform.service.OrganizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "Público", description = "Endpoints sin autenticación")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final OrganizationService organizationService;

    public PublicController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/organizations")
    public List<OrganizationResponse> listActiveOrganizations() {
        return organizationService.findActiveOrganizations();
    }
}
