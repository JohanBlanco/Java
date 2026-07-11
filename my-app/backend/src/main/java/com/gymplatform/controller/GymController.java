package com.gymplatform.controller;

import com.gymplatform.dto.*;
import com.gymplatform.service.*;
import com.gymplatform.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Gimnasio", description = "Usuarios, membresías, actividades, reservaciones y rutinas")
@RestController
@RequestMapping("/api")
public class GymController {

    private final UserService userService;
    private final MembershipPackageService packageService;
    private final ActivityService activityService;
    private final ReservationService reservationService;
    private final RoutineService routineService;

    private final MemberSubscriptionService memberSubscriptionService;
    private final GymStatsService gymStatsService;

    public GymController(UserService userService,
                         MembershipPackageService packageService, ActivityService activityService,
                         ReservationService reservationService, RoutineService routineService,
                         MemberSubscriptionService memberSubscriptionService,
                         GymStatsService gymStatsService) {
        this.userService = userService;
        this.packageService = packageService;
        this.activityService = activityService;
        this.reservationService = reservationService;
        this.routineService = routineService;
        this.memberSubscriptionService = memberSubscriptionService;
        this.gymStatsService = gymStatsService;
    }

    // --- Usuarios ---
    @Operation(summary = "Crear usuario del gimnasio",
            description = "Crea usuarios del gimnasio actual con uno o más roles (Admin, Recepcionista, Instructor, Miembro). "
                    + "Cada usuario pertenece solo a su organización. Password opcional (default 12345678).")
    @PostMapping("/users")
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        return userService.createStaff(SecurityUtils.requireOrganizationId(), request);
    }

    @GetMapping("/users")
    public List<UserResponse> getUsers() {
        return userService.findByOrganization(SecurityUtils.requireOrganizationId());
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Actualizar usuario del gimnasio",
            description = "Actualiza datos de un usuario del gimnasio. Password opcional: si se omite, no cambia.")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserCreateRequest request) {
        return userService.updateStaff(SecurityUtils.requireOrganizationId(), id, request);
    }

    @GetMapping("/users/me")
    public UserResponse getMyProfile() {
        return userService.getProfile(SecurityUtils.currentUser().getId());
    }

    @PutMapping("/users/me/profile")
    public UserResponse updateMyProfile(@Valid @RequestBody MemberProfileUpdateRequest request) {
        return userService.updateProfile(SecurityUtils.currentUser().getId(), request);
    }

    // --- Membresías ---
    @PostMapping("/packages")
    public MembershipPackageResponse createPackage(@Valid @RequestBody MembershipPackageRequest request) {
        return packageService.create(SecurityUtils.requireOrganizationId(), request);
    }

    @GetMapping("/packages")
    public List<MembershipPackageResponse> getPackages() {
        return packageService.findByOrganization(SecurityUtils.requireOrganizationId());
    }

    @GetMapping("/packages/{id}")
    public MembershipPackageResponse getPackage(@PathVariable Long id) {
        return packageService.findById(SecurityUtils.requireOrganizationId(), id);
    }

    @PutMapping("/packages/{id}")
    public MembershipPackageResponse updatePackage(@PathVariable Long id, @Valid @RequestBody MembershipPackageRequest request) {
        return packageService.update(SecurityUtils.requireOrganizationId(), id, request);
    }

    // --- Actividades ---
    @PostMapping("/activities")
    public ActivityResponse createActivity(@Valid @RequestBody ActivityRequest request) {
        return activityService.create(SecurityUtils.requireOrganizationId(), request);
    }

    @GetMapping("/activities")
    public List<ActivityResponse> getActivities(
            @RequestParam(required = false) java.time.LocalDate from,
            @RequestParam(required = false) java.time.LocalDate to,
            @RequestParam(required = false, defaultValue = "false") boolean series) {
        Long orgId = SecurityUtils.requireOrganizationId();
        if (series) {
            return activityService.findSeries(orgId);
        }
        return activityService.findByOrganization(orgId, from, to);
    }

    @GetMapping("/activities/{id}")
    public ActivityResponse getActivity(@PathVariable Long id) {
        return activityService.findById(SecurityUtils.requireOrganizationId(), id);
    }

    @PutMapping("/activities/{id}")
    public ActivityResponse updateActivity(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return activityService.update(SecurityUtils.requireOrganizationId(), id, request);
    }

    @PostMapping("/activities/{id}/reservation-impact/preview")
    @Operation(summary = "Vista previa de reservaciones afectadas al editar")
    public ActivityReservationImpactResponse previewActivityUpdateImpact(
            @PathVariable Long id,
            @Valid @RequestBody ActivityRequest request) {
        return activityService.previewUpdateImpact(SecurityUtils.requireOrganizationId(), id, request);
    }

    @GetMapping("/activities/{id}/reservation-impact")
    @Operation(summary = "Reservaciones activas antes de eliminar")
    public ActivityReservationImpactResponse getActivityDeleteImpact(@PathVariable Long id) {
        return activityService.getDeleteImpact(SecurityUtils.requireOrganizationId(), id);
    }

    @DeleteMapping("/activities/{id}")
    @Operation(summary = "Eliminar actividad (desactivar serie)")
    public void deleteActivity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean cancelReservations) {
        activityService.delete(SecurityUtils.requireOrganizationId(), id, cancelReservations);
    }

    @PutMapping("/activities/{id}/occurrence-edit")
    @Operation(summary = "Editar ocurrencia desde calendario",
            description = "scope=OCCURRENCE aplica solo a la fecha; scope=SERIES actualiza toda la serie.")
    public ActivityResponse editActivityOccurrence(
            @PathVariable Long id,
            @Valid @RequestBody ActivityOccurrenceEditRequest request) {
        return activityService.editOccurrence(SecurityUtils.requireOrganizationId(), id, request);
    }

    // --- Reservaciones ---
    @PostMapping("/activities/{activityId}/reservations")
    public ReservationResponse createReservation(
            @PathVariable Long activityId,
            @RequestBody(required = false) ReservationCreateRequest request) {
        return reservationService.create(activityId, SecurityUtils.currentUser().getId(), request);
    }

    @PostMapping("/reservations/{id}/confirm")
    public ReservationResponse confirmReservation(@PathVariable Long id) {
        return reservationService.confirm(id);
    }

    @PostMapping("/reservations/{id}/cancel")
    public ReservationResponse cancelReservation(@PathVariable Long id) {
        return reservationService.cancel(id);
    }

    @GetMapping("/reservations/me")
    public List<ReservationResponse> myReservations() {
        return reservationService.findByMember(SecurityUtils.currentUser().getId());
    }

    @GetMapping("/reservations/pending-payment")
    @Operation(summary = "Reservaciones con pago pendiente", description = "Lista reservaciones que requieren pago en recepción.")
    public List<ReservationResponse> pendingPaymentReservations() {
        return reservationService.findPendingPayments(SecurityUtils.requireOrganizationId());
    }

    @GetMapping("/activities/{activityId}/reservations")
    public List<ReservationResponse> activityReservations(@PathVariable Long activityId) {
        return reservationService.findByActivity(activityId);
    }

    @PostMapping("/reservations/{id}/mark-paid")
    @Operation(summary = "Marcar reservación como pagada", description = "Recepción marca el pago antes de confirmar la reservación.")
    public ReservationResponse markReservationPaid(@PathVariable Long id) {
        return reservationService.markPaid(id);
    }

    @GetMapping("/sales")
    @Operation(summary = "Ventas registradas", description = "Pagos de actividades marcados en recepción.")
    public List<SaleResponse> getSales() {
        return gymStatsService.findSales(SecurityUtils.requireOrganizationId());
    }

    @GetMapping("/stats/summary")
    @Operation(summary = "Estadísticas del gimnasio")
    public GymStatsResponse getStatsSummary() {
        return gymStatsService.getSummary(SecurityUtils.requireOrganizationId());
    }

    @GetMapping("/users/me/membership-usage")
    public MembershipUsageResponse myMembershipUsage() {
        return memberSubscriptionService.getUsage(SecurityUtils.currentUser().getId());
    }

    @PostMapping("/users/{userId}/membership")
    public void assignMembership(@PathVariable Long userId, @RequestBody java.util.Map<String, Long> body) {
        Long packageId = body.get("membershipPackageId");
        if (packageId == null) {
            throw new com.gymplatform.exception.BusinessException("membershipPackageId es requerido");
        }
        memberSubscriptionService.assignMembership(SecurityUtils.requireOrganizationId(), userId, packageId);
    }

    // --- Rutinas ---
    @PostMapping("/routine-templates")
    public RoutineTemplateResponse createTemplate(@Valid @RequestBody RoutineTemplateRequest request) {
        var user = SecurityUtils.currentUser();
        return routineService.createTemplate(SecurityUtils.requireOrganizationId(), user.getId(), request);
    }

    @GetMapping("/routine-templates")
    public List<RoutineTemplateResponse> getTemplates() {
        return routineService.findTemplates(SecurityUtils.requireOrganizationId());
    }

    @PostMapping("/routines")
    public RoutineResponse createRoutine(@Valid @RequestBody CreateRoutineRequest request) {
        var user = SecurityUtils.currentUser();
        return routineService.createRoutine(SecurityUtils.requireOrganizationId(), user.getId(), request);
    }

    @PostMapping("/routines/assign-template")
    public List<RoutineResponse> assignTemplate(@Valid @RequestBody AssignTemplateRequest request) {
        var user = SecurityUtils.currentUser();
        return routineService.assignTemplate(SecurityUtils.requireOrganizationId(), user.getId(), request);
    }

    @GetMapping("/routines/me")
    public List<RoutineResponse> myRoutines() {
        return routineService.findByMember(SecurityUtils.currentUser().getId());
    }

    @PostMapping("/routine-requests")
    public RoutineRequestResponse createRoutineRequest(@Valid @RequestBody RoutineRequestCreate request) {
        return routineService.createRequest(
                SecurityUtils.requireOrganizationId(),
                SecurityUtils.currentUser().getId(),
                request
        );
    }

    @GetMapping("/routine-requests")
    public List<RoutineRequestResponse> getRoutineRequests() {
        return routineService.findRequests(SecurityUtils.requireOrganizationId());
    }

    @PutMapping("/routine-requests/{id}/status")
    public RoutineRequestResponse updateRoutineRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody RoutineRequestStatusUpdate request) {
        return routineService.updateRequestStatus(
                id,
                request.status(),
                SecurityUtils.currentUser().getId()
        );
    }
}
