package com.gymplatform.repository;

import com.gymplatform.domain.entity.RoutineRequest;
import com.gymplatform.domain.enums.RoutineRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoutineRequestRepository extends JpaRepository<RoutineRequest, Long> {
    List<RoutineRequest> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    List<RoutineRequest> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<RoutineRequest> findByOrganizationIdAndStatus(Long organizationId, RoutineRequestStatus status);
}
