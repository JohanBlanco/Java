package com.gymplatform.repository;



import com.gymplatform.domain.entity.Activity;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

import java.util.List;



public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByOrganizationIdAndActiveTrueOrderByStartDateAscStartTimeAsc(Long organizationId);

    @Query("""
            SELECT a FROM Activity a
            WHERE a.organization.id = :orgId AND a.active = false AND a.endDate >= :today
            ORDER BY a.startDate DESC, a.startTime DESC
            """)
    List<Activity> findEmergencyCancelledByOrganization(
            @Param("orgId") Long orgId, @Param("today") LocalDate today);

    @Query("""
            SELECT a FROM Activity a
            WHERE a.organization.id = :orgId AND a.endDate < :before
            """)
    List<Activity> findExpiredByOrganization(@Param("orgId") Long orgId, @Param("before") LocalDate before);

    @Query("""
            SELECT a FROM Activity a
            WHERE a.endDate < :before
            """)
    List<Activity> findAllExpired(@Param("before") LocalDate before);



    @Query("""

            SELECT a FROM Activity a

            WHERE a.organization.id = :orgId

            AND a.active = true

            AND a.startDate <= :to

            AND a.endDate >= :from

            ORDER BY a.startDate ASC, a.startTime ASC

            """)

    List<Activity> findActiveSeriesOverlapping(
            @Param("orgId") Long orgId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT a FROM Activity a
            WHERE a.organization.id = :orgId
            AND a.startDate <= :to
            AND a.endDate >= :from
            ORDER BY a.startDate ASC, a.startTime ASC
            """)
    List<Activity> findSeriesOverlapping(
            @Param("orgId") Long orgId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

}

