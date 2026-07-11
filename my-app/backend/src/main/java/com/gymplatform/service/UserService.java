package com.gymplatform.service;

import com.gymplatform.domain.entity.MemberProfile;
import com.gymplatform.domain.entity.Organization;
import com.gymplatform.domain.entity.User;
import com.gymplatform.domain.enums.Role;
import com.gymplatform.dto.MemberProfileUpdateRequest;
import com.gymplatform.dto.UserCreateRequest;
import com.gymplatform.dto.UserResponse;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.MemberProfileRepository;
import com.gymplatform.repository.OrganizationRepository;
import com.gymplatform.repository.UserRepository;
import com.gymplatform.util.PasswordHelper;
import com.gymplatform.util.RoleUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberSubscriptionService memberSubscriptionService;

    public UserService(UserRepository userRepository, MemberProfileRepository memberProfileRepository,
                       OrganizationRepository organizationRepository, PasswordEncoder passwordEncoder,
                       MemberSubscriptionService memberSubscriptionService) {
        this.userRepository = userRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberSubscriptionService = memberSubscriptionService;
    }

    @Transactional
    public UserResponse createStaff(Long organizationId, UserCreateRequest request) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));

        Set<Role> roles = RoleUtils.normalizeGymRoles(request.roles());

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo ya está registrado");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(PasswordHelper.resolve(request.password())));
        user.setRoles(roles);
        user.setOrganization(org);

        user = userRepository.save(user);
        ensureMemberProfile(user, request);
        assignMembershipIfRequested(organizationId, user, request);
        return UserMapper.toResponse(user, memberProfileRepository.findByUserId(user.getId()).orElse(null));
    }

    @Transactional
    public UserResponse updateStaff(Long organizationId, Long userId, UserCreateRequest request) {
        User user = requireStaffUser(organizationId, userId);
        Set<Role> roles = RoleUtils.normalizeGymRoles(request.roles());

        if (!request.email().equals(user.getEmail()) && userRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo ya está registrado");
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setRoles(roles);

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(PasswordHelper.resolve(request.password())));
        }

        user = userRepository.save(user);
        ensureMemberProfile(user, request);
        assignMembershipIfRequested(organizationId, user, request);
        return UserMapper.toResponse(user, memberProfileRepository.findByUserId(userId).orElse(null));
    }

    private void assignMembershipIfRequested(Long organizationId, User user, UserCreateRequest request) {
        if (request.membershipPackageId() != null && user.hasRole(Role.MEMBER)) {
            memberSubscriptionService.assignMembership(organizationId, user.getId(), request.membershipPackageId());
        }
    }

    private void ensureMemberProfile(User user, UserCreateRequest request) {
        if (!user.hasRole(Role.MEMBER)) {
            return;
        }
        MemberProfile profile = memberProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    MemberProfile p = new MemberProfile();
                    p.setUser(user);
                    return p;
                });
        if (request.birthYear() != null) profile.setBirthYear(request.birthYear());
        if (request.age() != null) profile.setAge(request.age());
        if (request.goals() != null) profile.setGoals(request.goals());
        if (request.phone() != null) profile.setPhone(request.phone());
        profile.setUpdatedAt(Instant.now());
        memberProfileRepository.save(profile);
    }

    private User requireStaffUser(Long organizationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (user.getOrganization() == null || !user.getOrganization().getId().equals(organizationId)) {
            throw new BusinessException("El usuario no pertenece a este gimnasio");
        }
        if (RoleUtils.isPlatformUser(user.getRoles())) {
            throw new BusinessException("No se puede editar este usuario");
        }
        return user;
    }

    public java.util.Optional<User> findGymOwner(Long organizationId) {
        return userRepository.findByOrganizationIdAndRole(organizationId, Role.GYM_OWNER).stream().findFirst();
    }

    @Transactional
    public void syncGymOwner(Long organizationId, String firstName, String lastName, String email, String password) {
        var owners = userRepository.findByOrganizationIdAndRole(organizationId, Role.GYM_OWNER);
        User owner = owners.isEmpty() ? null : owners.get(0);

        if (owner == null) {
            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
            createStaff(organizationId, new UserCreateRequest(
                    firstName != null && !firstName.isBlank() ? firstName : "Administrador",
                    lastName != null && !lastName.isBlank() ? lastName : org.getName(),
                    email,
                    password,
                    List.of(Role.GYM_OWNER),
                    null, null, null, null, null
            ));
            return;
        }

        if (firstName != null && !firstName.isBlank()) {
            owner.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            owner.setLastName(lastName);
        }
        if (email != null && !email.equals(owner.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException("El correo ya está registrado");
            }
            owner.setEmail(email);
        }
        if (password != null && !password.isBlank()) {
            owner.setPasswordHash(passwordEncoder.encode(PasswordHelper.resolve(password)));
        }
        if (!owner.hasRole(Role.GYM_OWNER)) {
            owner.getRoles().add(Role.GYM_OWNER);
        }
        userRepository.save(owner);
    }

    public List<UserResponse> findByOrganization(Long organizationId) {
        return userRepository.findByOrganizationId(organizationId).stream()
                .map(u -> UserMapper.toResponse(u, memberProfileRepository.findByUserId(u.getId()).orElse(null)))
                .toList();
    }

    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        MemberProfile profile = memberProfileRepository.findByUserId(userId).orElse(null);
        return UserMapper.toResponse(user, profile);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, MemberProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        MemberProfile profile = memberProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    MemberProfile p = new MemberProfile();
                    p.setUser(user);
                    return p;
                });

        if (request.birthYear() != null) profile.setBirthYear(request.birthYear());
        if (request.age() != null) profile.setAge(request.age());
        if (request.goals() != null) profile.setGoals(request.goals());
        if (request.phone() != null) profile.setPhone(request.phone());
        if (request.emergencyContact() != null) profile.setEmergencyContact(request.emergencyContact());
        profile.setUpdatedAt(Instant.now());

        profile = memberProfileRepository.save(profile);
        return UserMapper.toResponse(user, profile);
    }
}
