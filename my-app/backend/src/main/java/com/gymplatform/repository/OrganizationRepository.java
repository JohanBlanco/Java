package com.gymplatform.repository;

import com.gymplatform.domain.entity.Organization;
import com.gymplatform.domain.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findBySlug(String slug);
    List<Organization> findBySubscriptionStatus(SubscriptionStatus status);
    List<Organization> findByActiveTrue();
}
