package com.gymplatform.config;

import com.gymplatform.domain.entity.*;
import com.gymplatform.domain.enums.*;
import com.gymplatform.repository.*;
import com.gymplatform.service.StaffAvailabilityService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MembershipPackageRepository packageRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final ActivityRepository activityRepository;
    private final ReservationRepository reservationRepository;
    private final RoutineRequestRepository routineRequestRepository;
    private final AppointmentRequestRepository appointmentRequestRepository;
    private final StaffAvailabilityRepository staffAvailabilityRepository;
    private final StaffAvailabilityService staffAvailabilityService;
    private final RoutineRepository routineRepository;
    private final RoutineTemplateRepository routineTemplateRepository;
    private final BodyMeasurementRepository bodyMeasurementRepository;
    private final NutritionPlanRepository nutritionPlanRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, OrganizationRepository organizationRepository,
                      MemberProfileRepository memberProfileRepository,
                      MembershipPackageRepository packageRepository,
                      MemberSubscriptionRepository subscriptionRepository,
                      ActivityRepository activityRepository,
                      ReservationRepository reservationRepository,
                      RoutineRequestRepository routineRequestRepository,
                      AppointmentRequestRepository appointmentRequestRepository,
                      StaffAvailabilityRepository staffAvailabilityRepository,
                      StaffAvailabilityService staffAvailabilityService,
                      RoutineRepository routineRepository,
                      RoutineTemplateRepository routineTemplateRepository,
                      BodyMeasurementRepository bodyMeasurementRepository,
                      NutritionPlanRepository nutritionPlanRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.packageRepository = packageRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.activityRepository = activityRepository;
        this.reservationRepository = reservationRepository;
        this.routineRequestRepository = routineRequestRepository;
        this.appointmentRequestRepository = appointmentRequestRepository;
        this.staffAvailabilityRepository = staffAvailabilityRepository;
        this.staffAvailabilityService = staffAvailabilityService;
        this.routineRepository = routineRepository;
        this.routineTemplateRepository = routineTemplateRepository;
        this.bodyMeasurementRepository = bodyMeasurementRepository;
        this.nutritionPlanRepository = nutritionPlanRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            seedPlatformOwner();
        }
        organizationRepository.findBySlug("fitlife").ifPresentOrElse(
                this::seedFitLifeDemo,
                () -> seedFitLifeFromScratch()
        );
        seedPowerGymIfMissing();
        seedIronFitIfMissing();
        printCredentials();
    }

    private void seedPlatformOwner() {
        userRepository.findByEmail("admin@gymplatform.com").ifPresentOrElse(
                user -> ensureUserNationalId(user, "109990001"),
                () -> {
        User platformOwner = new User();
        platformOwner.setFirstName("Admin");
        platformOwner.setLastName("Plataforma");
        platformOwner.setEmail("admin@gymplatform.com");
        platformOwner.setPasswordHash(passwordEncoder.encode("admin123"));
                    platformOwner.setNationalId("109990001");
        platformOwner.setRoles(Set.of(Role.PLATFORM_OWNER));
        userRepository.save(platformOwner);
                });
    }

    private void seedFitLifeFromScratch() {
        Organization org = new Organization();
        org.setName("FitLife Gym");
        org.setSlug("fitlife");
        org.setContactEmail("contacto@fitlife.com");
        org.setContactPhone("555-0100");
        org.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        seedFitLifeDemo(organizationRepository.save(org));
    }

    private void seedFitLifeDemo(Organization org) {
        User instructor = ensureStaff(org, "Ana", "Torres", "instructor@fitlife.com", "instructor123",
                Set.of(Role.INSTRUCTOR), "203451234");
        User receptionist = ensureStaff(org, "María", "López", "recepcion@fitlife.com", "recepcion123",
                Set.of(Role.RECEPTIONIST, Role.MEMBER), "305672345");
        User gymOwner = ensureStaff(org, "Carlos", "Mendoza", "dueno@fitlife.com", "12345678",
                Set.of(Role.GYM_OWNER, Role.INSTRUCTOR, Role.MEMBER), "104560123");

        ensureMemberProfile(gymOwner, 1985, "Mantener condición física y liderar el gym", "555-0101", "104560123");
        ensureMemberProfile(receptionist, 1990, "Bienestar general", "555-0102", "305672345");

        MembershipPackage basicPkg = ensurePackage(org, "Membresía Básica",
                "Acceso a área de pesas y cardio", new BigDecimal("599.00"), 1, 4, true, true);
        MembershipPackage premiumPkg = ensurePackage(org, "Membresía Premium",
                "Acceso completo + clases ilimitadas", new BigDecimal("899.00"), 1, null, false, true);
        ensurePackage(org, "Plan Anual Legacy",
                "Plan descontinuado — solo referencia histórica", new BigDecimal("4999.00"), 12, 2, false, false);

        // --- Miembros: distintos estados de membresía y cuenta ---
        User luis = ensureMember(org, "Luis", "García", "miembro@fitlife.com", "miembro123",
                1995, "Ganar masa muscular y mejorar resistencia", "555-0200", "1-9020-5678");
        User patricia = ensureMember(org, "Patricia", "Ruiz", "patricia@fitlife.com", "miembro123",
                1992, "Bajar de peso y tonificar", "555-0201", "2-0345-6789");
        User roberto = ensureMember(org, "Roberto", "Sánchez", "roberto@fitlife.com", "miembro123",
                1988, "Mejorar resistencia cardiovascular", "555-0202", "1-1122-3344");
        User sofia = ensureMember(org, "Sofía", "Hernández", "sofia@fitlife.com", "miembro123",
                2000, "Flexibilidad y bienestar general", "555-0203", "3-0456-7890");
        User diego = ensureMember(org, "Diego", "Morales", "diego@fitlife.com", "miembro123",
                1987, "Retomar entrenamiento después de pausa", "555-0204", "1-2233-4455");
        User elena = ensureMember(org, "Elena", "Castillo", "elena@fitlife.com", "miembro123",
                1993, "Preparación para competencia local", "555-0205", "2-0567-8901");
        User hector = ensureMember(org, "Héctor", "Navarro", "hector@fitlife.com", "miembro123",
                1991, "Recuperación post-lesión de hombro", "555-0206", "1-3344-5566");
        ensureInactiveMember(org, "Carmen", "Vega", "carmen@fitlife.com", "miembro123",
                1984, "Cuenta suspendida por administración", "555-0207", "2-0678-9012");

        // ACTIVO vigente
        ensureSubscription(luis, basicPkg, LocalDate.now().minusDays(10), LocalDate.now().plusMonths(1));
        ensureSubscription(gymOwner, premiumPkg, LocalDate.now(), LocalDate.now().plusMonths(1));
        ensureSubscription(receptionist, basicPkg, LocalDate.now(), LocalDate.now().plusMonths(1));
        // ACTIVO por vencer (5 días)
        ensureSubscription(elena, premiumPkg, LocalDate.now().minusDays(25), LocalDate.now().plusDays(5));
        // PENDIENTE DE PAGO — vencido reciente y moroso
        ensureSubscription(patricia, basicPkg, LocalDate.now().minusMonths(2), LocalDate.now().minusDays(20));
        ensureSubscription(hector, basicPkg, LocalDate.now().minusMonths(1), LocalDate.now().minusDays(10));
        ensureSubscription(roberto, premiumPkg, LocalDate.now().minusMonths(3), LocalDate.now().minusDays(45));
        // INACTIVO — más de 2 meses vencido
        ensureSubscription(diego, basicPkg, LocalDate.now().minusMonths(5), LocalDate.now().minusMonths(3));
        // Sofía y Carmen: sin membresía

        seedActivities(org, instructor);
        seedReservations(org, luis, patricia, roberto, elena, hector, receptionist);
        seedRoutineTemplates(org, instructor);
        seedRoutines(org, instructor, luis, patricia);
        seedRoutineRequests(org, instructor, luis, patricia, roberto, hector, sofia);
        seedAppointmentRequests(org, instructor, luis, patricia, roberto, elena, sofia, diego);
        seedStaffAvailability(org);
        seedBodyMeasurements(org, instructor, luis, patricia, roberto, sofia, elena, hector);
        seedNutritionPlans(org, instructor, luis, patricia, elena);
    }

    private void seedPowerGymIfMissing() {
        if (organizationRepository.findBySlug("powergym").isPresent()) return;

        Organization org = new Organization();
        org.setName("Power Gym");
        org.setSlug("powergym");
        org.setContactEmail("contacto@powergym.com");
        org.setContactPhone("555-0300");
        org.setSubscriptionStatus(SubscriptionStatus.TRIAL);
        org = organizationRepository.save(org);

        ensureStaff(org, "Jorge", "Ramírez", "admin@powergym.com", "12345678", Set.of(Role.GYM_OWNER), "204560001");
        User instructor = ensureStaff(org, "Laura", "Vega", "instructor@powergym.com", "12345678",
                Set.of(Role.INSTRUCTOR), "204560002");
        ensurePackage(org, "Plan Mensual", "Acceso general al gimnasio", new BigDecimal("499.00"), 1, 6, false, true);
        ensureActivity(org, instructor, "CrossFit", "WOD del día",
                LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), "Box principal", 18);
    }

    private void seedIronFitIfMissing() {
        if (organizationRepository.findBySlug("ironfit").isPresent()) return;

        Organization org = new Organization();
        org.setName("Iron Fit");
        org.setSlug("ironfit");
        org.setContactEmail("contacto@ironfit.com");
        org.setContactPhone("555-0400");
        org.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
        org.setActive(false);
        org = organizationRepository.save(org);

        ensureStaff(org, "Pedro", "Silva", "admin@ironfit.com", "12345678", Set.of(Role.GYM_OWNER), "204560003");
    }

    private User ensureStaff(Organization org, String firstName, String lastName, String email,
                              String password, Set<Role> roles, String nationalId) {
        String normalized = com.gymplatform.util.NationalIdHelper.normalize(nationalId);
        return userRepository.findByEmail(email).map(user -> {
            ensureUserNationalId(user, normalized);
            return user;
        }).orElseGet(() -> {
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRoles(roles);
            user.setOrganization(org);
            user.setNationalId(normalized);
            return userRepository.save(user);
        });
    }

    private void ensureUserNationalId(User user, String normalizedNationalId) {
        if (normalizedNationalId == null || !com.gymplatform.util.NationalIdHelper.isValid(normalizedNationalId)) {
            return;
        }
        if (user.getNationalId() == null || user.getNationalId().isBlank()) {
            user.setNationalId(normalizedNationalId);
            userRepository.save(user);
        }
    }

    private User ensureMember(Organization org, String firstName, String lastName, String email,
                              String password, int birthYear, String goals, String phone, String nationalId) {
        User member = userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRoles(Set.of(Role.MEMBER));
            user.setOrganization(org);
            user.setActive(true);
            return userRepository.save(user);
        });
        ensureMemberProfile(member, birthYear, goals, phone, nationalId);
        return member;
    }

    private User ensureInactiveMember(Organization org, String firstName, String lastName, String email,
                                      String password, int birthYear, String goals, String phone, String nationalId) {
        User member = userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRoles(Set.of(Role.MEMBER));
            user.setOrganization(org);
            user.setActive(false);
            return userRepository.save(user);
        });
        ensureMemberProfile(member, birthYear, goals, phone, nationalId);
        return member;
    }

    private void ensureMemberProfile(User member, int birthYear, String goals, String phone, String nationalId) {
        MemberProfile profile = memberProfileRepository.findByUserId(member.getId()).orElseGet(() -> {
            MemberProfile p = new MemberProfile();
            p.setUser(member);
            return p;
        });
        if (profile.getBirthYear() == null) profile.setBirthYear(birthYear);
        if (profile.getAge() == null) profile.setAge(LocalDate.now().getYear() - birthYear);
        if (profile.getGoals() == null) profile.setGoals(goals);
        if (profile.getPhone() == null) profile.setPhone(phone);
        if (profile.getNationalId() == null && nationalId != null) {
            String normalized = com.gymplatform.util.NationalIdHelper.normalize(nationalId);
            profile.setNationalId(normalized);
            ensureUserNationalId(member, normalized);
        }
        profile.setUpdatedAt(Instant.now());
        memberProfileRepository.save(profile);
    }

    private MembershipPackage ensurePackage(Organization org, String name, String description,
                                            BigDecimal price, int durationMonths, Integer freeQuota,
                                            boolean withAddons, boolean active) {
        return packageRepository.findByOrganizationIdAndActiveTrue(org.getId()).stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst()
                .or(() -> packageRepository.findAll().stream()
                        .filter(p -> p.getOrganization().getId().equals(org.getId()) && name.equals(p.getName()))
                        .findFirst())
                .orElseGet(() -> createPackage(org, name, description, price, durationMonths, freeQuota, withAddons, active));
    }

    private MembershipPackage createPackage(Organization org, String name, String description,
                                            BigDecimal price, int durationMonths, Integer freeQuota,
                                            boolean withAddons, boolean active) {
        MembershipPackage pkg = new MembershipPackage();
        pkg.setName(name);
        pkg.setDescription(description);
        pkg.setPrice(price);
        pkg.setDurationMonths(durationMonths);
        pkg.setFreeActivityQuota(freeQuota);
        pkg.setActive(active);
        pkg.setOrganization(org);

        if (withAddons) {
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
        }

        return packageRepository.save(pkg);
    }

    private void ensureSubscription(User member, MembershipPackage pkg, LocalDate start, LocalDate end) {
        if (subscriptionRepository.findFirstByMemberIdOrderByStartDateDesc(member.getId()).isPresent()) {
            return;
        }
        MemberSubscription subscription = new MemberSubscription();
        subscription.setMember(member);
        subscription.setMembershipPackage(pkg);
        subscription.setStartDate(start);
        subscription.setEndDate(end);
        subscription.setActive(true);
        subscriptionRepository.save(subscription);
    }

    private void seedActivities(Organization org, User instructor) {
        // Hoy — distintos horarios y cupos
        ensureActivity(org, instructor, "Spinning matutino", "Ciclismo indoor de alta intensidad",
                LocalDate.now(), LocalTime.of(7, 30), LocalTime.of(8, 30), "Sala 1", 15);
        ensureActivity(org, instructor, "Funcional", "Entrenamiento funcional de hoy",
                LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), "Sala 2", 20);
        ensureActivity(org, instructor, "HIIT Grupal", "Circuito de alta intensidad",
                LocalDate.now(), LocalTime.of(11, 30), LocalTime.of(12, 30), "Sala 2", 16);
        ensureActivity(org, instructor, "Zumba", "Baile fitness en grupo",
                LocalDate.now(), LocalTime.of(14, 0), LocalTime.of(15, 0), "Sala 1", 25);
        ensureActivity(org, instructor, "Boxeo tarde", "Técnica y acondicionamiento",
                LocalDate.now(), LocalTime.of(16, 0), LocalTime.of(17, 0), "Sala 4", 12);
        ensureActivity(org, instructor, "Yoga al atardecer", "Sesión de flexibilidad — cupo ilimitado",
                LocalDate.now(), LocalTime.of(18, 0), LocalTime.of(19, 0), "Terraza", null);
        ensureActivity(org, instructor, "Spinning nocturno", "Ciclismo indoor vespertino",
                LocalDate.now(), LocalTime.of(19, 30), LocalTime.of(20, 30), "Sala 1", 15);

        // Futuras
        ensureActivity(org, instructor, "Spinning", "Clase grupal de ciclismo indoor",
                LocalDate.now().plusDays(2), LocalTime.of(7, 0), LocalTime.of(8, 0), "Sala 1", 15);
        ensureActivity(org, instructor, "Yoga", "Sesión de flexibilidad y respiración",
                LocalDate.now().plusDays(3), LocalTime.of(18, 30), LocalTime.of(19, 30), "Terraza", null);
        ensureActivity(org, instructor, "HIIT Express", "Alta intensidad en 45 minutos",
                LocalDate.now().plusDays(5), LocalTime.of(19, 0), LocalTime.of(19, 45), "Sala 2", 10);

        // Ayer — actividad pasada (calendario)
        ensureActivity(org, instructor, "Boxeo fitness", "Combinación de técnica y cardio",
                LocalDate.now().minusDays(1), LocalTime.of(17, 0), LocalTime.of(18, 0), "Sala 4", 12);

        // Recurrente
        if (activityRepository.findByOrganizationIdAndActiveTrueOrderByStartDateAscStartTimeAsc(org.getId()).stream()
                .noneMatch(a -> "Pilates".equals(a.getName()) && a.isRecurring())) {
        Activity pilates = new Activity();
        pilates.setName("Pilates");
            pilates.setDescription("Clase recurrente lun/mié/vie");
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
        }
    }

    private Activity ensureActivity(Organization org, User instructor, String name, String description,
                                    LocalDate date, LocalTime start, LocalTime end, String location, Integer capacity) {
        return activityRepository.findActiveSeriesOverlapping(org.getId(), date, date).stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElseGet(() -> activityRepository
                        .findByOrganizationIdAndActiveTrueOrderByStartDateAscStartTimeAsc(org.getId())
                        .stream()
                        .filter(a -> name.equals(a.getName()) && !a.isRecurring())
                        .findFirst()
                        .map(existing -> {
                            existing.setStartDate(date);
                            existing.setEndDate(date);
                            existing.setStartTime(start);
                            existing.setEndTime(end);
                            existing.setDescription(description);
                            existing.setLocationName(location);
                            existing.setCapacity(capacity);
                            existing.setInstructor(instructor);
                            return activityRepository.save(existing);
                        })
                        .orElseGet(() -> saveActivity(org, instructor, name, description, date, start, end, location, capacity)));
    }

    private Activity saveActivity(Organization org, User instructor, String name, String description,
                                  LocalDate date, LocalTime start, LocalTime end, String location, Integer capacity) {
        Activity activity = new Activity();
        activity.setName(name);
        activity.setDescription(description);
        activity.setStartDate(date);
        activity.setEndDate(date);
        activity.setRecurring(false);
        activity.setStartTime(start);
        activity.setEndTime(end);
        activity.setLocationName(location);
        activity.setCapacity(capacity);
        activity.setInstructor(instructor);
        activity.setOrganization(org);
        return activityRepository.save(activity);
    }

    private void seedReservations(Organization org, User luis, User patricia, User roberto,
                                  User elena, User hector, User receptionist) {
        Activity funcional = findActivity(org, "Funcional", LocalDate.now());
        Activity spinning = findActivity(org, "Spinning matutino", LocalDate.now());
        Activity yogaToday = findActivity(org, "Yoga al atardecer", LocalDate.now());
        Activity yogaFuture = findActivity(org, "Yoga", LocalDate.now().plusDays(3));
        Activity boxeoAyer = findActivity(org, "Boxeo fitness", LocalDate.now().minusDays(1));
        Activity hiit = findActivity(org, "HIIT Express", LocalDate.now().plusDays(5));

        // Luis — cupo gratis confirmado + asistió
        if (funcional != null) {
            ensureReservation(funcional, luis, LocalDate.now(), ReservationStatus.CONFIRMED,
                    true, false, false, true);
        }
        // Luis — pago pendiente en recepción
        if (spinning != null) {
            ensureReservation(spinning, luis, LocalDate.now(), ReservationStatus.CONFIRMED,
                    false, true, false, false);
        }
        // Luis — pagado y confirmado (ventas)
        if (yogaFuture != null) {
            ensureReservation(yogaFuture, luis, LocalDate.now().plusDays(3), ReservationStatus.CONFIRMED,
                    false, true, true, false);
        }
        // Luis — canceló reservación
        if (yogaToday != null) {
            ensureReservation(yogaToday, luis, LocalDate.now(), ReservationStatus.CANCELLED,
                    false, false, false, false);
        }

        // Patricia (morosa) — confirmada sin pagar actividad
        if (spinning != null) {
            ensureReservation(spinning, patricia, LocalDate.now(), ReservationStatus.CONFIRMED,
                    false, true, false, false);
        }

        // Roberto — pagado hoy (ventas del día)
        if (spinning != null) {
            ensureReservation(spinning, roberto, LocalDate.now(), ReservationStatus.CONFIRMED,
                    false, true, true, false, Instant.now());
        }

        // Elena — confirmada gratis, aún no asiste
        if (funcional != null) {
            ensureReservation(funcional, elena, LocalDate.now(), ReservationStatus.CONFIRMED,
                    true, false, false, false);
        }

        // Héctor — pendiente de confirmar + pago
        if (hiit != null) {
            ensureReservation(hiit, hector, LocalDate.now().plusDays(5), ReservationStatus.CONFIRMED,
                    false, true, false, false);
        }

        // Recepcionista — pagó y asistió ayer
        if (boxeoAyer != null) {
            ensureReservation(boxeoAyer, receptionist, LocalDate.now().minusDays(1), ReservationStatus.CONFIRMED,
                    false, true, true, true, Instant.now().minus(1, ChronoUnit.DAYS));
        }

        // Roberto — venta del mes pasado
        if (boxeoAyer != null) {
            ensureReservation(boxeoAyer, roberto, LocalDate.now().minusDays(1), ReservationStatus.CONFIRMED,
                    false, true, true, true, Instant.now().minus(20, ChronoUnit.DAYS));
        }
    }

    private Activity findActivity(Organization org, String name, LocalDate date) {
        return activityRepository.findActiveSeriesOverlapping(org.getId(), date, date).stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElse(null);
    }

    private void ensureReservation(Activity activity, User member, LocalDate occurrenceDate,
                                   ReservationStatus status, boolean freeSlot, boolean paymentRequired,
                                   boolean paid, boolean attended) {
        ensureReservation(activity, member, occurrenceDate, status, freeSlot, paymentRequired, paid, attended,
                paid ? Instant.now() : null);
    }

    private void ensureReservation(Activity activity, User member, LocalDate occurrenceDate,
                                   ReservationStatus status, boolean freeSlot, boolean paymentRequired,
                                   boolean paid, boolean attended, Instant updatedAt) {
        if (reservationRepository.existsByActivityIdAndMemberIdAndOccurrenceDateAndStatusIn(
                activity.getId(), member.getId(), occurrenceDate,
                List.of(ReservationStatus.CONFIRMED))) {
            return;
        }

        Reservation reservation = new Reservation();
        reservation.setActivityName(activity.getName());
        reservation.setMember(member);
        reservation.setOccurrenceDate(occurrenceDate);
        reservation.setStatus(status);
        reservation.setFreeSlot(freeSlot);
        reservation.setPaymentRequired(paymentRequired);
        reservation.setPaid(paid);
        reservation.setAttended(attended);
        if (status == ReservationStatus.CANCELLED) {
            reservation.setActivity(null);
        } else {
            reservation.setActivity(activity);
        }
        if (updatedAt != null) {
            reservation.setUpdatedAt(updatedAt);
        }
        reservationRepository.save(reservation);
    }

    private void seedRoutineTemplates(Organization org, User instructor) {
        ensureRoutineTemplate(org, instructor, "Full body principiante", "Adaptación y fuerza general",
                List.of(
                        ex("Sentadilla", 3, 12, "0", 0),
                        ex("Press banca", 3, 10, "0", 0),
                        ex("Remo con mancuerna", 3, 12, "0", 0)
                ));
        ensureRoutineTemplate(org, instructor, "Pierna y glúteo", "Hipertrofia tren inferior",
                List.of(
                        ex("Prensa 45°", 4, 12, "40 kg", 0),
                        ex("Peso muerto rumano", 3, 10, "30 kg", 0),
                        ex("Zancadas", 3, 10, "0", 0)
                ));
    }

    private void ensureRoutineTemplate(Organization org, User instructor, String name, String goal,
                                       List<RoutineExercise> exercises) {
        boolean exists = routineTemplateRepository.findByOrganizationIdAndActiveTrue(org.getId()).stream()
                .anyMatch(t -> name.equals(t.getName()));
        if (exists) return;

        RoutineTemplate template = new RoutineTemplate();
        template.setName(name);
        template.setDescription("Plantilla demo: " + name);
        template.setGoal(goal);
        template.setInstructor(instructor);
        template.setOrganization(org);
        for (int i = 0; i < exercises.size(); i++) {
            RoutineExercise ex = exercises.get(i);
            ex.setTemplate(template);
            ex.setOrderIndex(i);
            template.getExercises().add(ex);
        }
        routineTemplateRepository.save(template);
    }

    private void seedRoutines(Organization org, User instructor, User luis, User patricia) {
        ensureRoutine(org, instructor, luis, "Rutina de hipertrofia", false,
                "Tren superior y core", List.of(
                        ex("Press inclinado", 4, 10, "20 kg", 0),
                        ex("Jalón al pecho", 4, 12, "35 kg", 0),
                        ex("Plancha abdominal", 3, 1, "0", 45)
                ));
        ensureRoutine(org, instructor, patricia, "Rutina express temporal", true,
                "Solo 2 semanas — enfoque cardio", List.of(
                        ex("Elíptica", 1, 1, "0", 1200),
                        ex("Burpees", 3, 15, "0", 0)
                ));
    }

    private void ensureRoutine(Organization org, User instructor, User member, String name, boolean temporary,
                               String description, List<RoutineExercise> exercises) {
        boolean exists = routineRepository.findByMemberIdAndActiveTrue(member.getId()).stream()
                .anyMatch(r -> name.equals(r.getName()));
        if (exists) return;

        Routine routine = new Routine();
        routine.setName(name);
        routine.setDescription(description);
        routine.setMember(member);
        routine.setInstructor(instructor);
        routine.setOrganization(org);
        routine.setTemporary(temporary);
        for (int i = 0; i < exercises.size(); i++) {
            RoutineExercise ex = exercises.get(i);
            ex.setRoutine(routine);
            ex.setOrderIndex(i);
            routine.getExercises().add(ex);
        }
        routineRepository.save(routine);
    }

    private RoutineExercise ex(String name, int sets, int reps, String weight, int durationSeconds) {
        RoutineExercise ex = new RoutineExercise();
        ex.setExerciseName(name);
        ex.setSets(sets);
        ex.setReps(reps);
        ex.setWeight(weight);
        ex.setDurationSeconds(durationSeconds > 0 ? durationSeconds : null);
        return ex;
    }

    private void seedRoutineRequests(Organization org, User instructor, User luis, User patricia,
                                       User roberto, User hector, User sofia) {
        ensureRoutineRequest(org, luis, "Quiero una rutina de fuerza",
                "Hipertrofia en tren superior", RoutineRequestStatus.PENDING, null, instructor,
                "Prefiero entrenar por la mañana");
        ensureRoutineRequest(org, patricia, "Rutina para perder grasa",
                "Definición y cardio moderado", RoutineRequestStatus.PENDING, null, null, null);
        ensureRoutineRequest(org, hector, "Plan post-lesión de hombro",
                "Movilidad y fortalecimiento progresivo", RoutineRequestStatus.IN_PROGRESS, instructor, instructor,
                "Evitar press vertical por ahora");
        ensureRoutineRequest(org, roberto, "Rutina completada — mantenimiento",
                "Consolidar avances del trimestre", RoutineRequestStatus.COMPLETED, instructor, instructor, null);
        ensureRoutineRequest(org, sofia, "Solicitud rechazada — fuera de alcance",
                "Preparación powerlifting competencia", RoutineRequestStatus.REJECTED, null, instructor, null);
    }

    private void ensureRoutineRequest(Organization org, User member, String description, String goals,
                                      RoutineRequestStatus status, User assignedInstructor,
                                      User preferredInstructor, String additionalNotes) {
        boolean exists = routineRequestRepository.findByOrganizationIdOrderByCreatedAtDesc(org.getId()).stream()
                .anyMatch(r -> r.getMember() != null
                        && r.getMember().getId().equals(member.getId()) && description.equals(r.getDescription()));
        if (exists) return;

        RoutineRequest request = new RoutineRequest();
        request.setMember(member);
        request.setOrganization(org);
        request.setDescription(description);
        request.setGoals(goals);
        request.setAdditionalNotes(additionalNotes);
        request.setStatus(status);
        request.setAssignedInstructor(assignedInstructor);
        request.setPreferredInstructor(preferredInstructor != null ? preferredInstructor : assignedInstructor);
        if (status == RoutineRequestStatus.COMPLETED) {
            request.setCompletedAt(java.time.Instant.now());
        }
        routineRequestRepository.save(request);
    }

    private void seedStaffAvailability(Organization org) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 14; i++) {
            LocalDate date = today.plusDays(i);
            if (date.getDayOfWeek().getValue() >= 6) continue;
            ensureAvailability(org, date, LocalTime.of(9, 0), LocalTime.of(13, 0), 30);
            ensureAvailability(org, date, LocalTime.of(16, 0), LocalTime.of(19, 0), 30);
        }
    }

    private void ensureAvailability(Organization org, LocalDate date, LocalTime start, LocalTime end,
                                    Integer slotMinutes) {
        var existing = staffAvailabilityRepository
                .findByOrganizationIdAndStaffIsNullAndAvailabilityDateOrderByStartTimeAsc(org.getId(), date)
                .stream()
                .filter(a -> a.getStartTime().equals(start) && a.getEndTime().equals(end))
                .findFirst();
        if (existing.isPresent()) {
            StaffAvailability av = existing.get();
            if (slotMinutes != null && !java.util.Objects.equals(av.getSlotDurationMinutes(), slotMinutes)) {
                av.setSlotDurationMinutes(slotMinutes);
                staffAvailabilityRepository.save(av);
            }
            staffAvailabilityService.syncOpenAppointments(av);
            return;
        }

        StaffAvailability availability = new StaffAvailability();
        availability.setOrganization(org);
        availability.setStaff(null);
        availability.setAvailabilityDate(date);
        availability.setStartTime(start);
        availability.setEndTime(end);
        availability.setSlotDurationMinutes(slotMinutes);
        StaffAvailability saved = staffAvailabilityRepository.save(availability);
        staffAvailabilityService.syncOpenAppointments(saved);
    }

    private void seedAppointmentRequests(Organization org, User instructor, User luis, User patricia, User roberto,
                                         User elena, User sofia, User diego) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        ensureAppointmentRequest(org, luis, instructor, AppointmentType.MEASUREMENT,
                "Primera toma de medidas del mes", AppointmentRequestStatus.SCHEDULED,
                tomorrow.atTime(9, 0).atZone(zone).toInstant(),
                tomorrow.atTime(9, 30).atZone(zone).toInstant());
        ensureAppointmentRequest(org, patricia, instructor, AppointmentType.NUTRITION,
                "Consulta nutricional — plan de déficit calórico", AppointmentRequestStatus.SCHEDULED,
                tomorrow.atTime(10, 0).atZone(zone).toInstant(),
                tomorrow.atTime(10, 30).atZone(zone).toInstant());
        ensureAppointmentRequest(org, roberto, instructor, AppointmentType.CONSULTATION,
                "Seguimiento de objetivos y lesión de rodilla", AppointmentRequestStatus.SCHEDULED,
                tomorrow.atTime(16, 0).atZone(zone).toInstant(),
                tomorrow.atTime(16, 30).atZone(zone).toInstant());

        LocalDate dayAfter = LocalDate.now().plusDays(2);
        ensureAppointmentRequest(org, elena, instructor, AppointmentType.MEASUREMENT,
                "Control pre-competencia agendado", AppointmentRequestStatus.SCHEDULED,
                dayAfter.atTime(9, 30).atZone(zone).toInstant(),
                dayAfter.atTime(10, 0).atZone(zone).toInstant());
        ensureAppointmentRequest(org, luis, instructor, AppointmentType.CONSULTATION,
                "Revisión de rutina con instructor", AppointmentRequestStatus.SCHEDULED,
                dayAfter.atTime(11, 0).atZone(zone).toInstant(),
                dayAfter.atTime(11, 30).atZone(zone).toInstant());

        ensureAppointmentRequest(org, diego, instructor, AppointmentType.NUTRITION,
                "Consulta nutricional completada en enero", AppointmentRequestStatus.COMPLETED,
                LocalDate.now().minusDays(3).atTime(10, 0).atZone(zone).toInstant(),
                LocalDate.now().minusDays(3).atTime(10, 30).atZone(zone).toInstant());

        ensureAppointmentRequest(org, sofia, instructor, AppointmentType.CONSULTATION,
                "Canceló por viaje de trabajo", AppointmentRequestStatus.CANCELLED,
                LocalDate.now().minusDays(1).atTime(17, 0).atZone(zone).toInstant(),
                LocalDate.now().minusDays(1).atTime(18, 0).atZone(zone).toInstant());
    }

    private void ensureAppointmentRequest(Organization org, User member, User preferredStaff,
                                          AppointmentType type, String notes, AppointmentRequestStatus status,
                                          Instant scheduledStart, Instant scheduledEnd) {
        boolean exists = appointmentRequestRepository.findByOrganizationIdOrderByCreatedAtDesc(org.getId()).stream()
                .anyMatch(r -> r.getMember() != null
                        && r.getMember().getId().equals(member.getId()) && notes.equals(r.getNotes()));
        if (exists) return;

        AppointmentRequest request = new AppointmentRequest();
        request.setMember(member);
        request.setOrganization(org);
        request.setType(type);
        request.setNotes(notes);
        request.setStatus(status);
        request.setPreferredStaff(preferredStaff);
        if (status == AppointmentRequestStatus.SCHEDULED || status == AppointmentRequestStatus.COMPLETED) {
            request.setAssignedStaff(preferredStaff);
        }
        request.setScheduledStart(scheduledStart);
        request.setScheduledEnd(scheduledEnd);
        linkStaffAvailability(org, request, scheduledStart, scheduledEnd);
        appointmentRequestRepository.save(request);
    }

    private void linkStaffAvailability(Organization org, AppointmentRequest request,
                                       Instant scheduledStart, Instant scheduledEnd) {
        if (scheduledStart == null || scheduledEnd == null) return;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate date = LocalDate.ofInstant(scheduledStart, zone);
        LocalTime start = LocalTime.ofInstant(scheduledStart, zone);
        LocalTime end = LocalTime.ofInstant(scheduledEnd, zone);
        staffAvailabilityRepository
                .findByOrganizationIdAndStaffIsNullAndAvailabilityDateOrderByStartTimeAsc(org.getId(), date)
                .stream()
                .filter(a -> !start.isBefore(a.getStartTime()) && !end.isAfter(a.getEndTime()))
                .findFirst()
                .ifPresent(request::setStaffAvailability);
    }

    private void seedBodyMeasurements(Organization org, User instructor, User luis, User patricia,
                                      User roberto, User sofia, User elena, User hector) {
        Instant now = Instant.now();

        // Luis — evolución de 3 meses: bajó grasa, subió masa (objetivo hipertrofia)
        ensureBodyMeasurement(org, instructor, luis, "demo:luis-1", now.minus(90, ChronoUnit.DAYS),
                31, BiologicalSex.MALE, 82.0, 178.0,
                39.0, 100.0, 92.0, null, 118.0,
                33.0, 33.5, 28.0, 28.5, 58.0, 58.5, 38.0, 38.5,
                "Línea base — inicio de plan de fuerza");
        ensureBodyMeasurement(org, instructor, luis, "demo:luis-2", now.minus(45, ChronoUnit.DAYS),
                31, BiologicalSex.MALE, 79.5, 178.0,
                38.5, 102.0, 88.0, null, 119.0,
                34.0, 34.5, 28.5, 29.0, 57.0, 57.5, 38.5, 39.0,
                "Control intermedio — buena adherencia al plan");
        ensureBodyMeasurement(org, instructor, luis, "demo:luis-3", now.minus(7, ChronoUnit.DAYS),
                31, BiologicalSex.MALE, 77.0, 178.0,
                38.0, 105.0, 84.0, null, 120.0,
                35.0, 35.5, 29.0, 29.5, 56.0, 56.5, 39.0, 39.5,
                "Recomposition visible — brazos y pecho en alza");

        // Patricia — mujer en déficit: progreso claro en cintura y peso
        ensureBodyMeasurement(org, instructor, patricia, "demo:patricia-1", now.minus(60, ChronoUnit.DAYS),
                34, BiologicalSex.FEMALE, 72.0, 162.0,
                32.0, 92.0, 78.0, 102.0, 98.0,
                28.0, 28.5, 24.0, 24.5, 54.0, 54.5, 36.0, 36.5,
                "Evaluación inicial — enfoque tonificación");
        ensureBodyMeasurement(org, instructor, patricia, "demo:patricia-2", now.minus(15, ChronoUnit.DAYS),
                34, BiologicalSex.FEMALE, 68.5, 162.0,
                31.0, 89.0, 74.0, 98.0, 96.0,
                27.5, 28.0, 23.5, 24.0, 52.5, 53.0, 35.5, 36.0,
                "Seguimiento — cintura -4 cm, buen ritmo de pérdida");

        // Roberto — perfil atlético estable
        ensureBodyMeasurement(org, instructor, roberto, "demo:roberto-1", now.minus(30, ChronoUnit.DAYS),
                38, BiologicalSex.MALE, 75.0, 172.0,
                37.0, 98.0, 78.0, 96.0, 112.0,
                32.0, 32.5, 27.0, 27.5, 55.0, 55.5, 37.0, 37.5,
                "Control cardiovascular — composición atlética");

        // Sofía — peso normal, enfoque bienestar
        ensureBodyMeasurement(org, instructor, sofia, "demo:sofia-1", now.minus(20, ChronoUnit.DAYS),
                26, BiologicalSex.FEMALE, 58.0, 165.0,
                30.0, 86.0, 65.0, 90.0, 94.0,
                26.0, 26.5, 22.0, 22.5, 50.0, 50.5, 34.0, 34.5,
                "Primera toma — flexibilidad y condición general");

        // Elena — preparación competencia: muy definida
        ensureBodyMeasurement(org, instructor, elena, "demo:elena-1", now.minus(40, ChronoUnit.DAYS),
                33, BiologicalSex.FEMALE, 64.0, 168.0,
                31.5, 88.0, 68.0, 92.0, 98.0,
                27.0, 27.5, 23.0, 23.5, 53.0, 53.5, 35.0, 35.5,
                "Pre-competencia — fase de definición");
        ensureBodyMeasurement(org, instructor, elena, "demo:elena-2", now.minus(5, ChronoUnit.DAYS),
                33, BiologicalSex.FEMALE, 62.5, 168.0,
                31.0, 87.0, 66.0, 91.0, 97.0,
                26.5, 27.0, 22.5, 23.0, 52.0, 52.5, 34.5, 35.0,
                "Semana de competencia — pico de forma");

        // Héctor — sobrepeso, recuperación post-lesión
        ensureBodyMeasurement(org, instructor, hector, "demo:hector-1", now.minus(25, ChronoUnit.DAYS),
                35, BiologicalSex.MALE, 88.0, 175.0,
                40.0, 104.0, 96.0, 100.0, 116.0,
                31.0, 31.0, 27.0, 27.0, 57.0, 57.0, 38.0, 38.0,
                "Retorno gradual — evitar carga en hombro derecho");
    }

    private void ensureBodyMeasurement(Organization org, User instructor, User member, String seedKey,
                                       Instant measuredAt, int ageYears, BiologicalSex sex,
                                       double weightKg, double heightCm,
                                       Double neckCm, Double chestCm, Double waistCm, Double hipsCm, Double shouldersCm,
                                       Double leftArmCm, Double rightArmCm,
                                       Double leftForearmCm, Double rightForearmCm,
                                       Double leftThighCm, Double rightThighCm,
                                       Double leftCalfCm, Double rightCalfCm,
                                       String notes) {
        boolean exists = bodyMeasurementRepository
                .findByOrganizationIdAndMemberIdOrderByMeasuredAtDesc(org.getId(), member.getId())
                .stream()
                .anyMatch(m -> m.getNotes() != null && m.getNotes().startsWith(seedKey));
        if (exists) return;

        BodyMeasurement m = new BodyMeasurement();
        m.setOrganization(org);
        m.setMember(member);
        m.setRecordedBy(instructor);
        m.setMeasuredAt(measuredAt);
        m.setAgeYears(ageYears);
        m.setSex(sex);
        m.setWeightKg(weightKg);
        m.setHeightCm(heightCm);
        m.setNeckCm(neckCm);
        m.setChestCm(chestCm);
        m.setWaistCm(waistCm);
        m.setHipsCm(hipsCm);
        m.setShouldersCm(shouldersCm);
        m.setLeftArmCm(leftArmCm);
        m.setRightArmCm(rightArmCm);
        m.setLeftForearmCm(leftForearmCm);
        m.setRightForearmCm(rightForearmCm);
        m.setLeftThighCm(leftThighCm);
        m.setRightThighCm(rightThighCm);
        m.setLeftCalfCm(leftCalfCm);
        m.setRightCalfCm(rightCalfCm);
        m.setNotes(seedKey + " — " + notes);
        bodyMeasurementRepository.save(m);
    }

    private void seedNutritionPlans(Organization org, User instructor, User luis, User patricia, User elena) {
        ensureNutritionPlan(org, instructor, luis, "demo:nutrition-luis",
                "Plan hipertrofia — Luis",
                "Superávit controlado para ganar masa muscular manteniendo grasa baja.",
                2800, 170, 320, 80, 3.0,
                "Prioriza proteína en cada comida\nEntrena 60–90 min antes del pre-entreno\nPesa alimentos la primera semana",
                "Ajustar calorías según progreso en medidas corporales.",
                List.of(
                        meal("Desayuno", "07:30", "Alta proteína para iniciar el día",
                                item("Avena", "80 g"), item("Huevos enteros", "3 uds"), item("Plátano", "1 mediano"), item("Almendras", "20 g")),
                        meal("Almuerzo", "13:00", null,
                                item("Pechuga de pollo", "180 g"), item("Arroz integral", "150 g cocido"), item("Ensalada mixta", "1 plato"), item("Aguacate", "1/2 uds")),
                        meal("Pre-entreno", "16:30", "30–45 min antes de entrenar",
                                item("Yogurt griego", "200 g"), item("Miel", "1 cda"), item("Café", "1 taza")),
                        meal("Cena", "20:00", null,
                                item("Salmón", "160 g"), item("Camote", "200 g"), item("Brócoli al vapor", "1 taza"))
                ));

        ensureNutritionPlan(org, instructor, patricia, "demo:nutrition-patricia",
                "Plan déficit — Patricia",
                "Déficit moderado para pérdida de grasa y tonificación.",
                1650, 120, 150, 55, 2.5,
                "Evita azúcares líquidos y ultraprocesados\nCamina 30 min diarios\nProteína en cada comida para saciedad",
                "Revisar en 2 semanas según medidas de cintura.",
                List.of(
                        meal("Desayuno", "08:00", null,
                                item("Tortilla de claras", "2 claras + 1 entero"), item("Pan integral", "1 rebanada"), item("Fresas", "1 taza")),
                        meal("Almuerzo", "13:30", null,
                                item("Atún en agua", "1 lata"), item("Quinoa", "120 g cocida"), item("Vegetales salteados", "1.5 taza")),
                        meal("Merienda", "16:00", null,
                                item("Manzana", "1 mediana"), item("Queso cottage", "100 g")),
                        meal("Cena", "19:30", "Cena ligera",
                                item("Pechuga de pavo", "150 g"), item("Ensalada verde", "1 plato grande"), item("Aceite de oliva", "1 cda"))
                ));

        ensureNutritionPlan(org, instructor, elena, "demo:nutrition-elena",
                "Plan competencia — Elena",
                "Mantenimiento en fase de definición pre-competencia.",
                1900, 140, 180, 60, 3.0,
                "Mantén horarios fijos de comida\nSuplementa electrolitos en entrenos largos",
                null,
                List.of(
                        meal("Desayuno", "07:00", null,
                                item("Avena", "60 g"), item("Proteína whey", "1 scoop"), item("Arándanos", "1/2 taza")),
                        meal("Almuerzo", "12:30", null,
                                item("Pescado blanco", "170 g"), item("Arroz basmati", "100 g cocido"), item("Espárragos", "1 taza")),
                        meal("Cena", "19:00", null,
                                item("Carne magra", "140 g"), item("Ensalada", "1 plato"), item("Aceite de oliva", "1 cda"))
                ));
    }

    private record MealSeed(String name, String time, String notes, List<ItemSeed> items) {}
    private record ItemSeed(String food, String portion) {}

    private MealSeed meal(String name, String time, String notes, ItemSeed... items) {
        return new MealSeed(name, time, notes, List.of(items));
    }

    private ItemSeed item(String food, String portion) {
        return new ItemSeed(food, portion);
    }

    private void ensureNutritionPlan(Organization org, User instructor, User member, String seedKey,
                                       String title, String objective,
                                       int calories, int protein, int carbs, int fat, double water,
                                       String guidelines, String notes, List<MealSeed> meals) {
        boolean exists = nutritionPlanRepository
                .findByOrganizationIdAndMemberIdOrderByCreatedAtDesc(org.getId(), member.getId())
                .stream()
                .anyMatch(p -> p.getNotes() != null && p.getNotes().startsWith(seedKey));
        if (exists) return;

        NutritionPlan plan = new NutritionPlan();
        plan.setOrganization(org);
        plan.setMember(member);
        plan.setCreatedBy(instructor);
        plan.setTitle(title);
        plan.setObjective(objective);
        plan.setDailyCaloriesTarget(calories);
        plan.setProteinGrams(protein);
        plan.setCarbsGrams(carbs);
        plan.setFatGrams(fat);
        plan.setWaterLiters(water);
        plan.setGuidelines(guidelines);
        plan.setNotes(notes != null ? seedKey + " — " + notes : seedKey);
        plan.setStatus(NutritionPlanStatus.ACTIVE);
        plan.setValidFrom(LocalDate.now().minusWeeks(2));
        plan.setValidUntil(LocalDate.now().plusMonths(2));

        for (int i = 0; i < meals.size(); i++) {
            MealSeed ms = meals.get(i);
            NutritionMeal meal = new NutritionMeal();
            meal.setPlan(plan);
            meal.setName(ms.name());
            meal.setSuggestedTime(ms.time());
            meal.setNotes(ms.notes());
            meal.setOrderIndex(i);
            for (int j = 0; j < ms.items().size(); j++) {
                ItemSeed is = ms.items().get(j);
                NutritionMealItem item = new NutritionMealItem();
                item.setMeal(meal);
                item.setFoodName(is.food());
                item.setPortion(is.portion());
                item.setOrderIndex(j);
                meal.getItems().add(item);
            }
            plan.getMeals().add(meal);
        }
        nutritionPlanRepository.save(plan);
    }

    private void printCredentials() {
        if (userRepository.count() == 0) return;
        System.out.println("=== Datos de prueba (GymPlatform) ===");
        System.out.println("Platform Owner: admin@gymplatform.com o 109990001 / admin123");
        System.out.println("--- FitLife ---");
        System.out.println("Gym Admin:      dueno@fitlife.com o 104560123 / 12345678");
        System.out.println("Recepcionista:  recepcion@fitlife.com o 305672345 / recepcion123");
        System.out.println("Instructor:     instructor@fitlife.com o 203451234 / instructor123");
        System.out.println("Activo:         miembro@fitlife.com / miembro123");
        System.out.println("Por vencer:     elena@fitlife.com / miembro123");
        System.out.println("Moroso reciente: hector@fitlife.com / miembro123");
        System.out.println("Moroso:         patricia@fitlife.com, roberto@fitlife.com / miembro123");
        System.out.println("Sin membresía:  sofia@fitlife.com / miembro123");
        System.out.println("Inactivo:       diego@fitlife.com / miembro123");
        System.out.println("Cuenta off:     carmen@fitlife.com / miembro123");
        System.out.println("--- Platform ---");
        System.out.println("Power Gym:      admin@powergym.com / 12345678 (TRIAL)");
        System.out.println("Iron Fit:       admin@ironfit.com / 12345678 (SUSPENDED)");
    }
}
