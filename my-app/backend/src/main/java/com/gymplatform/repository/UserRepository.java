package com.gymplatform.repository;

import com.gymplatform.domain.entity.User;
import com.gymplatform.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByOrganizationId(Long organizationId);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE u.organization.id = :organizationId AND r = :role")
    List<User> findByOrganizationIdAndRole(@Param("organizationId") Long organizationId, @Param("role") Role role);
}
