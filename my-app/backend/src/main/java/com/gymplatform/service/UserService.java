package com.gymplatform.service;

import com.gymplatform.domain.entity.MemberProfile;
import com.gymplatform.domain.entity.Organization;
import com.gymplatform.domain.entity.User;
import com.gymplatform.domain.enums.MemberMembershipStatus;
import com.gymplatform.domain.enums.Role;
import com.gymplatform.dto.MemberMembershipInfo;
import com.gymplatform.dto.MemberProfileUpdateRequest;
import com.gymplatform.dto.UserCreateRequest;
import com.gymplatform.dto.UserCreateResponse;
import com.gymplatform.dto.UserResponse;
import com.gymplatform.dto.WhatsappOutboundResponse;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.MemberProfileRepository;
import com.gymplatform.repository.OrganizationRepository;
import com.gymplatform.repository.UserRepository;
import com.gymplatform.util.PasswordHelper;
import com.gymplatform.util.NationalIdHelper;
import com.gymplatform.util.RoleUtils;
import com.gymplatform.util.WhatsAppPhoneHelper;
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
    private final CustomFormService customFormService;

    public UserService(UserRepository userRepository, MemberProfileRepository memberProfileRepository,
                       OrganizationRepository organizationRepository, PasswordEncoder passwordEncoder,
                       MemberSubscriptionService memberSubscriptionService,
                       CustomFormService customFormService) {
        this.userRepository = userRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberSubscriptionService = memberSubscriptionService;
        this.customFormService = customFormService;
    }

    @Transactional
    public UserCreateResponse createStaff(Long organizationId, UserCreateRequest request) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));

        Set<Role> roles = RoleUtils.normalizeGymRoles(request.roles());
        validateMemberRequirements(roles, request);

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
        applyNationalId(user, null, request.nationalId());
        if (request.whatsappPhone() != null && !request.whatsappPhone().isBlank()) {
            user.setWhatsappPhone(WhatsAppPhoneHelper.normalizeCostaRicaLocal(request.whatsappPhone()));
        }

        user = userRepository.save(user);
        ensureMemberProfile(user, request);
        assignMembershipIfRequested(organizationId, user, request);
        String whatsappUrl = maybeSendRegistrationForm(organizationId, roles, request, user);
        return new UserCreateResponse(toUserResponse(user), whatsappUrl);
    }

    private String maybeSendRegistrationForm(
            Long organizationId,
            Set<Role> roles,
            UserCreateRequest request,
            User user) {
        if (!roles.contains(Role.MEMBER)) {
            return null;
        }
        boolean shouldSend = request.sendRegistrationForm() == null
                || Boolean.TRUE.equals(request.sendRegistrationForm());
        if (!shouldSend) {
            return null;
        }
        if (user.getWhatsappPhone() == null || user.getWhatsappPhone().isBlank()) {
            return null;
        }
        return customFormService.buildRegistrationFormWhatsappUrl(organizationId, user).orElse(null);
    }

    @Transactional(readOnly = true)
    public WhatsappOutboundResponse resendRegistrationForm(Long organizationId, Long userId) {
        User user = requireStaffUser(organizationId, userId);
        if (!user.hasRole(Role.MEMBER)) {
            throw new BusinessException("Solo se puede reenviar el formulario de registro a miembros");
        }
        return customFormService.resendRegistrationFormViaWhatsApp(organizationId, user);
    }

    @Transactional
    public UserResponse updateStaff(Long organizationId, Long userId, UserCreateRequest request) {
        User user = requireStaffUser(organizationId, userId);
        Set<Role> roles = RoleUtils.normalizeGymRoles(request.roles());
        validateMemberRequirements(roles, request);

        if (!request.email().equals(user.getEmail()) && userRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo ya está registrado");
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setRoles(roles);
        applyNationalId(user, user.getId(), request.nationalId());
        if (request.whatsappPhone() != null && !request.whatsappPhone().isBlank()) {
            user.setWhatsappPhone(WhatsAppPhoneHelper.normalizeCostaRicaLocal(request.whatsappPhone()));
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(PasswordHelper.resolve(request.password())));
        }

        user = userRepository.save(user);
        ensureMemberProfile(user, request);
        assignMembershipIfRequested(organizationId, user, request);
        return toUserResponse(user);
    }

    private void assignMembershipIfRequested(Long organizationId, User user, UserCreateRequest request) {
        if (request.membershipPackageId() != null && user.hasRole(Role.MEMBER)) {
            memberSubscriptionService.assignMembership(organizationId, user.getId(), request.membershipPackageId());
        }
    }

    private void validateMemberRequirements(Set<Role> roles, UserCreateRequest request) {
        if (!roles.contains(Role.MEMBER)) {
            return;
        }
        if (request.membershipPackageId() == null) {
            throw new BusinessException("Debe seleccionar una membresía para el miembro");
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
        profile.setNationalId(user.getNationalId());
        if (request.birthYear() != null) profile.setBirthYear(request.birthYear());
        if (request.age() != null) profile.setAge(request.age());
        if (request.goals() != null) profile.setGoals(request.goals());
        if (request.phone() != null) profile.setPhone(request.phone());
        profile.setUpdatedAt(Instant.now());
        memberProfileRepository.save(profile);
    }

    private void applyNationalId(User user, Long userId, String rawNationalId) {
        if (rawNationalId == null || rawNationalId.isBlank()) {
            throw new BusinessException("La cédula es obligatoria");
        }
        String normalized = NationalIdHelper.normalize(rawNationalId);
        if (!NationalIdHelper.isValid(normalized)) {
            throw new BusinessException("La cédula debe tener 9 dígitos numéricos");
        }
        if (userRepository.existsByNationalIdExcluding(normalized, userId)) {
            throw new BusinessException("Ya existe un usuario con esa cédula");
        }
        user.setNationalId(normalized);
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
                    null, null, null, null, null,
                    String.format("8%08d", organizationId % 100_000_000L),
                    null, false
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
                .map(this::toUserResponse)
                .toList();
    }

    public List<UserResponse> findPendingMembershipPayment(Long organizationId) {
        return userRepository.findByOrganizationIdAndRole(organizationId, Role.MEMBER).stream()
                .filter(u -> memberSubscriptionService.getMembershipInfo(u.getId()).status()
                        == MemberMembershipStatus.PAYMENT_PENDING)
                .map(this::toUserResponse)
                .sorted(java.util.Comparator.comparing(
                        u -> u.nextPaymentDate() != null ? u.nextPaymentDate() : java.time.LocalDate.MIN))
                .toList();
    }

    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return toUserResponse(user);
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
        if (request.nationalId() != null) {
            applyNationalId(user, userId, request.nationalId());
            profile.setNationalId(user.getNationalId());
        }
        profile.setUpdatedAt(Instant.now());

        profile = memberProfileRepository.save(profile);
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        MemberProfile profile = memberProfileRepository.findByUserId(user.getId()).orElse(null);
        MemberMembershipInfo membershipInfo = user.hasRole(Role.MEMBER)
                ? memberSubscriptionService.getMembershipInfo(user.getId())
                : null;
        return UserMapper.toResponse(user, profile, membershipInfo);
    }
}
