package com.gymplatform.repository;

import com.gymplatform.domain.entity.StaffAvailability;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffAvailabilityRepository extends JpaRepository<StaffAvailability, Long> {
    List<StaffAvailability> findByOrganizationIdAndStaffIsNullAndAvailabilityDateOrderByStartTimeAsc(
            Long organizationId, LocalDate availabilityDate);

    List<StaffAvailability> findByOrganizationIdAndStaffIsNullAndAvailabilityDateBetweenOrderByAvailabilityDateAscStartTimeAsc(
            Long organizationId, LocalDate from, LocalDate to);

    boolean existsByOrganizationIdAndStaffIsNullAndAvailabilityDateAndStartTimeAndEndTime(
            Long organizationId, LocalDate availabilityDate, LocalTime startTime, LocalTime endTime);

    @Query("""
            SELECT s FROM StaffAvailability s
            WHERE s.organization.id = :orgId
              AND s.staff IS NULL
              AND s.startTime = :startTime
              AND s.endTime = :endTime
              AND ((:slotMinutes IS NULL AND s.slotDurationMinutes IS NULL)
                   OR s.slotDurationMinutes = :slotMinutes)
            ORDER BY s.availabilityDate ASC
            """)
    List<StaffAvailability> findMatchingBlocks(
            @Param("orgId") Long organizationId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("slotMinutes") Integer slotDurationMinutes);
}
