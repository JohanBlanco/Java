package com.gymplatform.service;

import com.gymplatform.domain.entity.Activity;
import com.gymplatform.domain.entity.MemberSubscription;
import com.gymplatform.domain.entity.Reservation;
import com.gymplatform.domain.entity.User;
import com.gymplatform.domain.enums.ReservationStatus;
import com.gymplatform.dto.ReservationCreateRequest;
import com.gymplatform.dto.ReservationResponse;
import com.gymplatform.util.SecurityUtils;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.ActivityRepository;
import com.gymplatform.repository.ReservationRepository;
import com.gymplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final MemberSubscriptionService memberSubscriptionService;
    private final ActivityService activityService;

    public ReservationService(ReservationRepository reservationRepository,
                              ActivityRepository activityRepository,
                              UserRepository userRepository,
                              MemberSubscriptionService memberSubscriptionService,
                              ActivityService activityService) {
        this.reservationRepository = reservationRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.memberSubscriptionService = memberSubscriptionService;
        this.activityService = activityService;
    }

    @Transactional
    public ReservationResponse create(Long activityId, Long memberId, ReservationCreateRequest request) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada"));
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado"));

        LocalDate occurrenceDate = resolveOccurrenceDate(activity, request);
        activityService.assertValidOccurrence(activity, occurrenceDate);

        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        if (reservationRepository.existsByActivityIdAndMemberIdAndOccurrenceDateAndStatusIn(
                activityId, memberId, occurrenceDate, activeStatuses)) {
            throw new BusinessException("Ya tienes una reservación activa para esta clase");
        }

        assertCapacityAvailable(activity, occurrenceDate);

        boolean payAtReception = request != null && Boolean.TRUE.equals(request.payAtReception());
        MemberSubscription subscription = memberSubscriptionService.getActiveSubscription(memberId)
                .orElseThrow(() -> new BusinessException("No tienes una membresía activa. Contacta a recepción."));

        boolean hasFreeRemaining = memberSubscriptionService.hasFreeSlotRemaining(memberId);

        Reservation reservation = new Reservation();
        reservation.setActivity(activity);
        reservation.setMember(member);
        reservation.setOccurrenceDate(occurrenceDate);

        if (hasFreeRemaining && !payAtReception) {
            reservation.setFreeSlot(true);
            reservation.setPaid(true);
            reservation.setPaymentRequired(false);
        } else if (payAtReception) {
            reservation.setFreeSlot(false);
            reservation.setPaid(false);
            reservation.setPaymentRequired(true);
        } else {
            throw new BusinessException(
                    "Has agotado tus actividades gratuitas incluidas en tu membresía ("
                            + subscription.getMembershipPackage().getName()
                            + "). Debes pagar en recepción antes de reservar."
            );
        }

        reservation.setStatus(ReservationStatus.PENDING);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse confirm(Long reservationId) {
        Reservation reservation = getById(reservationId);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("No se puede confirmar una reservación cancelada");
        }
        if (reservation.isPaymentRequired() && !reservation.isPaid()) {
            throw new BusinessException("Debes pagar en recepción antes de confirmar la reservación");
        }

        assertCapacityAvailable(reservation.getActivity(), reservation.getOccurrenceDate());

        if (reservation.isFreeSlot()) {
            MemberSubscription sub = memberSubscriptionService.getActiveSubscription(reservation.getMember().getId())
                    .orElseThrow(() -> new BusinessException("No tienes una membresía activa"));
            Integer quota = sub.getMembershipPackage().getFreeActivityQuota();
            if (quota != null) {
                long used = memberSubscriptionService.countUsedFreeActivities(reservation.getMember().getId(), sub);
                if (used >= quota) {
                    throw new BusinessException("Has agotado tus actividades gratuitas incluidas en tu membresía");
                }
            }
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAttended(true);
        reservation.setUpdatedAt(Instant.now());
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse markPaid(Long reservationId) {
        requireReceptionRole();
        Reservation reservation = getById(reservationId);
        assertSameOrganization(reservation, SecurityUtils.requireOrganizationId());
        if (!reservation.isPaymentRequired()) {
            throw new BusinessException("Esta reservación no requiere pago");
        }
        reservation.setPaid(true);
        reservation.setUpdatedAt(Instant.now());
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancel(Long reservationId) {
        Reservation reservation = getById(reservationId);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(Instant.now());
        return toResponse(reservationRepository.save(reservation));
    }

    public List<ReservationResponse> findByMember(Long memberId) {
        return reservationRepository.findByMemberId(memberId).stream()
                .map(this::toResponse).toList();
    }

    public List<ReservationResponse> findByActivity(Long activityId) {
        return reservationRepository.findByActivityId(activityId).stream()
                .map(this::toResponse).toList();
    }

    public List<ReservationResponse> findPendingPayments(Long organizationId) {
        requireReceptionRole();
        return reservationRepository.findPendingPaymentsByOrganization(organizationId).stream()
                .map(this::toResponse).toList();
    }

    private LocalDate resolveOccurrenceDate(Activity activity, ReservationCreateRequest request) {
        if (activity.isRecurring()) {
            if (request == null || request.occurrenceDate() == null) {
                throw new BusinessException("Indica la fecha de la clase para reservar");
            }
            return request.occurrenceDate();
        }
        if (request != null && request.occurrenceDate() != null) {
            return request.occurrenceDate();
        }
        return activity.getStartDate();
    }

    private void requireReceptionRole() {
        var user = SecurityUtils.currentUser();
        if (!user.hasRole("GYM_OWNER") && !user.hasRole("RECEPTIONIST")) {
            throw new BusinessException("No tienes permiso para esta acción");
        }
    }

    private void assertSameOrganization(Reservation reservation, Long organizationId) {
        if (!reservation.getActivity().getOrganization().getId().equals(organizationId)) {
            throw new BusinessException("No autorizado");
        }
    }

    private void assertCapacityAvailable(Activity activity, LocalDate occurrenceDate) {
        if (activity.getCapacity() != null) {
            long confirmed = reservationRepository.countByActivityIdAndOccurrenceDateAndStatus(
                    activity.getId(), occurrenceDate, ReservationStatus.CONFIRMED);
            long pending = reservationRepository.countByActivityIdAndOccurrenceDateAndStatus(
                    activity.getId(), occurrenceDate, ReservationStatus.PENDING);
            if (confirmed + pending >= activity.getCapacity()) {
                throw new BusinessException("La actividad ya alcanzó su cupo máximo para esta fecha");
            }
        }
    }

    private Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservación no encontrada"));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getActivity().getId(),
                reservation.getActivity().getName(),
                reservation.getOccurrenceDate(),
                reservation.getMember().getId(),
                reservation.getMember().getFirstName() + " " + reservation.getMember().getLastName(),
                reservation.getStatus(),
                reservation.isFreeSlot(),
                reservation.isPaymentRequired(),
                reservation.isPaid(),
                reservation.isAttended(),
                reservation.getCreatedAt()
        );
    }
}
