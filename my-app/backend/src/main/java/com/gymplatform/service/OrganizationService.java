package com.gymplatform.service;

import com.gymplatform.domain.entity.Organization;
import com.gymplatform.domain.entity.User;
import com.gymplatform.domain.enums.Role;
import com.gymplatform.domain.enums.SubscriptionStatus;
import com.gymplatform.dto.OrganizationRequest;
import com.gymplatform.dto.OrganizationResponse;
import com.gymplatform.dto.UserCreateRequest;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.OrganizationRepository;
import com.gymplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CustomFormService customFormService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               UserService userService,
                               CustomFormService customFormService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.customFormService = customFormService;
    }

    @Transactional
    public OrganizationResponse create(OrganizationRequest request) {
        if (organizationRepository.findBySlug(request.slug()).isPresent()) {
            throw new BusinessException("El slug ya está en uso");
        }
        if (userRepository.existsByEmail(request.ownerEmail())) {
            throw new BusinessException("El correo del administrador ya está registrado");
        }

        Organization org = new Organization();
        org.setName(request.name());
        org.setSlug(request.slug());
        org.setContactEmail(resolveContactEmail(request));
        org.setContactPhone(request.contactPhone());
        if (request.subscriptionStatus() != null) {
            org.setSubscriptionStatus(request.subscriptionStatus());
        }
        org = organizationRepository.save(org);
        customFormService.ensureMemberRegistrationForm(org.getId());

        userService.createStaff(org.getId(), new UserCreateRequest(
                request.ownerFirstName(),
                request.ownerLastName(),
                request.ownerEmail(),
                request.ownerPassword(),
                List.of(Role.GYM_OWNER),
                null, null, null, null, null,
                request.ownerNationalId(),
                null, false
        ));

        return toResponse(org);
    }

    public List<OrganizationResponse> findAll() {
        return organizationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<OrganizationResponse> findActiveOrganizations() {
        return organizationRepository.findByActiveTrue().stream()
                .filter(org -> org.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                        || org.getSubscriptionStatus() == SubscriptionStatus.TRIAL)
                .map(this::toResponse)
                .toList();
    }

    public OrganizationResponse findById(Long id) {
        return toResponse(getById(id));
    }

    @Transactional
    public OrganizationResponse update(Long id, OrganizationRequest request) {
        Organization org = getById(id);

        if (request.slug() != null && !request.slug().equals(org.getSlug())) {
            organizationRepository.findBySlug(request.slug())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new BusinessException("El slug ya está en uso");
                    });
            org.setSlug(request.slug());
        }
        if (request.name() != null) {
            org.setName(request.name());
        }
        org.setContactEmail(resolveContactEmail(request));
        if (request.contactPhone() != null) {
            org.setContactPhone(request.contactPhone());
        }
        if (request.subscriptionStatus() != null) {
            org.setSubscriptionStatus(request.subscriptionStatus());
        }

        org = organizationRepository.save(org);
        userService.syncGymOwner(
                org.getId(),
                request.ownerFirstName(),
                request.ownerLastName(),
                request.ownerEmail(),
                request.ownerPassword()
        );

        return toResponse(org);
    }

    private Organization getById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
    }

    private String resolveContactEmail(OrganizationRequest request) {
        if (request.contactEmail() != null && !request.contactEmail().isBlank()) {
            return request.contactEmail();
        }
        return request.ownerEmail();
    }

    private OrganizationResponse toResponse(Organization org) {
        User owner = userService.findGymOwner(org.getId()).orElse(null);
        return new OrganizationResponse(
                org.getId(), org.getName(), org.getSlug(),
                org.getContactEmail(), org.getContactPhone(),
                org.getSubscriptionStatus(), org.isActive(),
                org.getCreatedAt(),
                owner != null ? owner.getFirstName() : null,
                owner != null ? owner.getLastName() : null,
                owner != null ? owner.getEmail() : null
        );
    }
}
