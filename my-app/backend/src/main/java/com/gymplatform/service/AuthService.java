package com.gymplatform.service;

import com.gymplatform.domain.entity.*;
import com.gymplatform.domain.enums.Role;
import com.gymplatform.dto.*;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.*;
import com.gymplatform.util.RoleUtils;
import com.gymplatform.security.JwtTokenProvider;
import com.gymplatform.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                       UserRepository userRepository, OrganizationRepository organizationRepository,
                       MemberProfileRepository memberProfileRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = tokenProvider.generateToken(principal);

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                RoleUtils.toNames(user.getRoles()),
                user.getOrganization() != null ? user.getOrganization().getId() : null
        );
    }

    @Transactional
    public UserResponse registerMember(UserCreateRequest request, Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));

        if (!org.isActive() || org.getSubscriptionStatus().name().equals("INACTIVE")) {
            throw new BusinessException("La organización no tiene un sistema activo");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo ya está registrado");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(Role.MEMBER));
        user.setOrganization(org);

        user = userRepository.save(user);

        MemberProfile profile = new MemberProfile();
        profile.setUser(user);
        profile.setBirthYear(request.birthYear());
        profile.setAge(request.age());
        profile.setGoals(request.goals());
        profile.setPhone(request.phone());
        memberProfileRepository.save(profile);

        return UserMapper.toResponse(user, profile);
    }
}
