package com.gymplatform.repository;

import com.gymplatform.domain.entity.MemberSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {
    Optional<MemberSubscription> findFirstByMemberIdAndActiveTrueOrderByStartDateDesc(Long memberId);

    Optional<MemberSubscription> findFirstByMemberIdOrderByStartDateDesc(Long memberId);

    List<MemberSubscription> findByMemberIdAndActiveTrueOrderByStartDateAsc(Long memberId);
}
