package com.gymplatform.repository;

import com.gymplatform.domain.entity.MembershipPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MembershipPackageRepository extends JpaRepository<MembershipPackage, Long> {
    List<MembershipPackage> findByOrganizationIdAndActiveTrue(Long organizationId);
}
