package com.gymplatform.config;

import com.gymplatform.domain.entity.*;
import com.gymplatform.domain.enums.Role;
import com.gymplatform.domain.enums.SubscriptionStatus;
import com.gymplatform.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MembershipPackageRepository packageRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final ActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, OrganizationRepository organizationRepository,
                      MemberProfileRepository memberProfileRepository,
                      MembershipPackageRepository packageRepository,
                      MemberSubscriptionRepository subscriptionRepository,
                      ActivityRepository activityRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.packageRepository = packageRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.activityRepository = activityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        User platformOwner = new User();
        platformOwner.setFirstName("Admin");
        platformOwner.setLastName("Plataforma");
        platformOwner.setEmail("admin@gymplatform.com");
        platformOwner.setPasswordHash(passwordEncoder.encode("admin123"));
        platformOwner.setRoles(Set.of(Role.PLATFORM_OWNER));
        userRepository.save(platformOwner);

        Organization org = new Organization();
        org.setName("FitLife Gym");
        org.setSlug("fitlife");
        org.setContactEmail("contacto@fitlife.com");
        org.setContactPhone("555-0100");
        org.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        org = organizationRepository.save(org);

        User gymOwner = new User();
        gymOwner.setFirstName("Carlos");
        gymOwner.setLastName("Mendoza");
        gymOwner.setEmail("dueno@fitlife.com");
        gymOwner.setPasswordHash(passwordEncoder.encode("12345678"));
        gymOwner.setRoles(Set.of(Role.GYM_OWNER, Role.INSTRUCTOR, Role.MEMBER));
        gymOwner.setOrganization(org);
        userRepository.save(gymOwner);

        User instructor = new User();
        instructor.setFirstName("Ana");
        instructor.setLastName("Torres");
        instructor.setEmail("instructor@fitlife.com");
        instructor.setPasswordHash(passwordEncoder.encode("instructor123"));
        instructor.setRoles(Set.of(Role.INSTRUCTOR));
        instructor.setOrganization(org);
        userRepository.save(instructor);

        User receptionist = new User();
        receptionist.setFirstName("María");
        receptionist.setLastName("López");
        receptionist.setEmail("recepcion@fitlife.com");
        receptionist.setPasswordHash(passwordEncoder.encode("recepcion123"));
        receptionist.setRoles(Set.of(Role.RECEPTIONIST, Role.MEMBER));
        receptionist.setOrganization(org);
        userRepository.save(receptionist);

        User member = new User();
        member.setFirstName("Luis");
        member.setLastName("García");
        member.setEmail("miembro@fitlife.com");
        member.setPasswordHash(passwordEncoder.encode("miembro123"));
        member.setRoles(Set.of(Role.MEMBER));
        member.setOrganization(org);
        member = userRepository.save(member);

        MemberProfile profile = new MemberProfile();
        profile.setUser(member);
        profile.setBirthYear(1995);
        profile.setAge(31);
        profile.setGoals("Ganar masa muscular y mejorar resistencia");
        profile.setPhone("555-0200");
        memberProfileRepository.save(profile);

        MembershipPackage pkg = new MembershipPackage();
        pkg.setName("Membresía Básica");
        pkg.setDescription("Acceso a área de pesas y cardio");
        pkg.setPrice(new BigDecimal("599.00"));
        pkg.setDurationMonths(1);
        pkg.setFreeActivityQuota(4);
        pkg.setOrganization(org);

        PackageAddon addon = new PackageAddon();
        addon.setName("Clases grupales");
        addon.setDescription("Yoga, spinning y crossfit");
        addon.setPrice(new BigDecimal("200.00"));
        addon.setMembershipPackage(pkg);
        pkg.getAddons().add(addon);

        PackageAddon addon2 = new PackageAddon();
        addon2.setName("Entrenador personal");
        addon2.setDescription("4 sesiones al mes");
        addon2.setPrice(new BigDecimal("800.00"));
        addon2.setMembershipPackage(pkg);
        pkg.getAddons().add(addon2);

        pkg = packageRepository.save(pkg);

        MemberSubscription subscription = new MemberSubscription();
        subscription.setMember(member);
        subscription.setMembershipPackage(pkg);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(pkg.getDurationMonths()));
        subscription.setActive(true);
        subscriptionRepository.save(subscription);

        Activity spinning = new Activity();
        spinning.setName("Spinning");
        spinning.setDescription("Clase grupal de ciclismo indoor");
        spinning.setStartDate(LocalDate.now().plusDays(2));
        spinning.setEndDate(LocalDate.now().plusDays(2));
        spinning.setRecurring(false);
        spinning.setStartTime(LocalTime.of(7, 0));
        spinning.setEndTime(LocalTime.of(8, 0));
        spinning.setLocationName("Sala 1");
        spinning.setCapacity(15);
        spinning.setInstructor(instructor);
        spinning.setOrganization(org);
        activityRepository.save(spinning);

        Activity yoga = new Activity();
        yoga.setName("Yoga");
        yoga.setDescription("Sesión de flexibilidad y respiración");
        yoga.setStartDate(LocalDate.now().plusDays(3));
        yoga.setEndDate(LocalDate.now().plusDays(3));
        yoga.setRecurring(false);
        yoga.setStartTime(LocalTime.of(18, 30));
        yoga.setEndTime(LocalTime.of(19, 30));
        yoga.setLocationName("Terraza");
        yoga.setCapacity(null);
        yoga.setInstructor(instructor);
        yoga.setOrganization(org);
        activityRepository.save(yoga);

        Activity todayClass = new Activity();
        todayClass.setName("Funcional");
        todayClass.setDescription("Entrenamiento funcional de hoy");
        todayClass.setStartDate(LocalDate.now());
        todayClass.setEndDate(LocalDate.now());
        todayClass.setRecurring(false);
        todayClass.setStartTime(LocalTime.of(10, 0));
        todayClass.setEndTime(LocalTime.of(11, 0));
        todayClass.setLocationName("Sala 2");
        todayClass.setCapacity(20);
        todayClass.setInstructor(instructor);
        todayClass.setOrganization(org);
        activityRepository.save(todayClass);

        Activity pilates = new Activity();
        pilates.setName("Pilates");
        pilates.setDescription("Clase recurrente de pilates");
        pilates.setStartDate(LocalDate.now().minusWeeks(1));
        pilates.setEndDate(LocalDate.now().plusMonths(3));
        pilates.setRecurring(true);
        pilates.setRepeatDays("MONDAY,WEDNESDAY,FRIDAY");
        pilates.setStartTime(LocalTime.of(9, 0));
        pilates.setEndTime(LocalTime.of(10, 0));
        pilates.setLocationName("Sala 3");
        pilates.setCapacity(12);
        pilates.setInstructor(instructor);
        pilates.setOrganization(org);
        activityRepository.save(pilates);

        System.out.println("=== Datos de prueba cargados ===");
        System.out.println("Platform Owner: admin@gymplatform.com / admin123");
        System.out.println("Gym Admin:      dueno@fitlife.com / 12345678 (Admin + Instructor + Miembro)");
        System.out.println("Recepcionista:  recepcion@fitlife.com / recepcion123 (Recepcionista + Miembro)");
        System.out.println("Instructor:     instructor@fitlife.com / instructor123");
        System.out.println("Miembro:        miembro@fitlife.com / miembro123 (4 actividades gratis/mes)");
    }
}
