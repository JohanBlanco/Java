package com.gymplatform.service;

import com.gymplatform.domain.entity.MemberProfile;
import com.gymplatform.domain.entity.User;
import com.gymplatform.dto.MemberProfileResponse;
import com.gymplatform.dto.UserResponse;
import com.gymplatform.util.RoleUtils;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user, MemberProfile profile) {
        MemberProfileResponse profileResponse = null;
        if (profile != null) {
            profileResponse = new MemberProfileResponse(
                    profile.getId(), profile.getBirthYear(), profile.getAge(),
                    profile.getGoals(), profile.getPhone(), profile.getEmergencyContact()
            );
        }
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                RoleUtils.toNames(user.getRoles()),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.isActive(),
                user.getCreatedAt(),
                profileResponse
        );
    }
}
