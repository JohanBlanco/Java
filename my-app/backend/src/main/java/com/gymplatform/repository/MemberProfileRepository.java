package com.gymplatform.repository;

import com.gymplatform.domain.entity.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {
    Optional<MemberProfile> findByUserId(Long userId);
}
