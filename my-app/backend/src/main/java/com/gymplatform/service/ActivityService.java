package com.gymplatform.service;

import com.gymplatform.domain.entity.Activity;
import com.gymplatform.domain.entity.ActivityOccurrenceOverride;
import com.gymplatform.domain.entity.Organization;
import com.gymplatform.domain.entity.Reservation;
import com.gymplatform.domain.entity.User;
import com.gymplatform.domain.enums.ReservationStatus;
import com.gymplatform.dto.ActivityOccurrenceEditRequest;
import com.gymplatform.dto.ActivityRequest;
import com.gymplatform.dto.ActivityReservationImpactResponse;
import com.gymplatform.dto.ActivityReservationImpactResponse.AffectedReservationItem;
import com.gymplatform.dto.ActivityResponse;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.ActivityOccurrenceOverrideRepository;
import com.gymplatform.repository.ActivityRepository;
import com.gymplatform.repository.OrganizationRepository;
import com.gymplatform.repository.ReservationRepository;
import com.gymplatform.repository.UserRepository;
import com.gymplatform.util.ActivityRecurrenceUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityService {

    private static final int DEFAULT_EXPAND_DAYS = 90;
    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ActivityRepository activityRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ActivityOccurrenceOverrideRepository overrideRepository;

    public ActivityService(ActivityRepository activityRepository,
                           OrganizationRepository organizationRepository,
                           UserRepository userRepository,
                           ReservationRepository reservationRepository,
                           ActivityOccurrenceOverrideRepository overrideRepository) {
        this.activityRepository = activityRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.overrideRepository = overrideRepository;
    }

    @Transactional
    public ActivityResponse create(Long organizationId, ActivityRequest request) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));

        Activity activity = new Activity();
        applyRequest(activity, request);
        activity.setOrganization(org);
        return toSeriesResponse(activityRepository.save(activity));
    }

    @Transactional
    public ActivityResponse update(Long organizationId, Long id, ActivityRequest request) {
        Activity activity = requireActivity(organizationId, id);
        Activity preview = copySchedule(activity);
        applyRequest(preview, request);

        List<Reservation> affected = findAffectedReservations(activity.getId(), preview);
        if (!affected.isEmpty() && !Boolean.TRUE.equals(request.confirmAffectedReservations())) {
            throw new BusinessException(
                    "Este cambio afectará " + affected.size()
                            + " reservaciones activas. Confirma para cancelarlas y guardar."
            );
        }
        if (!affected.isEmpty()) {
            cancelReservations(affected);
        }

        applyRequest(activity, request);
        return toSeriesResponse(activityRepository.save(activity));
    }

    @Transactional
    public ActivityResponse editOccurrence(Long organizationId, Long id, ActivityOccurrenceEditRequest request) {
        Activity activity = requireActivity(organizationId, id);
        assertValidOccurrence(activity, request.occurrenceDate());

        if ("SERIES".equalsIgnoreCase(request.scope())) {
            ActivityRequest seriesRequest = toSeriesRequest(activity, request);
            ActivityResponse updated = update(organizationId, id, seriesRequest);
            overrideRepository.findByActivityIdAndOccurrenceDate(id, request.occurrenceDate())
                    .ifPresent(overrideRepository::delete);
            return updated;
        }

        if (!"OCCURRENCE".equalsIgnoreCase(request.scope())) {
            throw new BusinessException("Alcance inválido. Usa OCCURRENCE o SERIES.");
        }

        if (!activity.isRecurring()) {
            ActivityRequest singleRequest = toSeriesRequest(activity, request);
            return update(organizationId, id, singleRequest);
        }

        ActivityOccurrenceOverride override = overrideRepository
                .findByActivityIdAndOccurrenceDate(id, request.occurrenceDate())
                .orElseGet(ActivityOccurrenceOverride::new);
        override.setActivity(activity);
        override.setOccurrenceDate(request.occurrenceDate());
        override.setStartTime(request.startTime());
        override.setEndTime(request.endTime());
        override.setLocationName(request.locationName());
        override.setCapacity(request.capacity());
        override.setUpdatedAt(Instant.now());
        overrideRepository.save(override);

        return toOccurrenceResponse(activity, request.occurrenceDate(), override);
    }

    @Transactional
    public void delete(Long organizationId, Long id, boolean cancelReservations) {
        Activity activity = requireActivity(organizationId, id);
        List<Reservation> active = findActiveReservations(activity.getId());

        if (!active.isEmpty() && !cancelReservations) {
            throw new BusinessException(
                    "Esta actividad tiene " + active.size()
                            + " reservaciones activas. Confirma la cancelación para eliminarla."
            );
        }
        if (!active.isEmpty()) {
            cancelReservations(active);
        }

        activity.setActive(false);
        activityRepository.save(activity);
    }

    public ActivityReservationImpactResponse getDeleteImpact(Long organizationId, Long id) {
        requireActivity(organizationId, id);
        List<Reservation> active = findActiveReservations(id);
        return toImpact(active, active);
    }

    public ActivityReservationImpactResponse previewUpdateImpact(Long organizationId, Long id, ActivityRequest request) {
        Activity activity = requireActivity(organizationId, id);
        Activity preview = copySchedule(activity);
        applyRequest(preview, request);

        List<Reservation> active = findActiveReservations(activity.getId());
        List<Reservation> affected = findAffectedReservations(activity.getId(), preview);
        return toImpact(active, affected);
    }

    public List<ActivityResponse> findSeries(Long organizationId) {
        return activityRepository
                .findByOrganizationIdAndActiveTrueOrderByStartDateAscStartTimeAsc(organizationId)
                .stream()
                .map(this::toSeriesResponse)
                .toList();
    }

    public List<ActivityResponse> findByOrganization(Long organizationId, LocalDate from, LocalDate to) {
        LocalDate rangeFrom = from != null ? from : LocalDate.now();
        LocalDate rangeTo = to != null ? to : rangeFrom.plusDays(DEFAULT_EXPAND_DAYS);

        List<Activity> series = activityRepository.findActiveSeriesOverlapping(organizationId, rangeFrom, rangeTo);
        Map<Long, Map<LocalDate, ActivityOccurrenceOverride>> overrides =
                loadOverrides(series, rangeFrom, rangeTo);
        List<ActivityResponse> result = new ArrayList<>();

        for (Activity activity : series) {
            List<LocalDate> occurrences = ActivityRecurrenceUtil.expandOccurrences(
                    activity.getStartDate(),
                    activity.getEndDate(),
                    activity.isRecurring(),
                    activity.getRepeatDays(),
                    rangeFrom,
                    rangeTo
            );
            Map<LocalDate, ActivityOccurrenceOverride> activityOverrides =
                    overrides.getOrDefault(activity.getId(), Map.of());
            for (LocalDate occurrence : occurrences) {
                result.add(toOccurrenceResponse(activity, occurrence, activityOverrides.get(occurrence)));
            }
        }

        result.sort(Comparator
                .comparing(ActivityResponse::activityDate)
                .thenComparing(ActivityResponse::startTime));
        return result;
    }

    public ActivityResponse findById(Long organizationId, Long id) {
        return toSeriesResponse(requireActivity(organizationId, id));
    }

    public void assertValidOccurrence(Activity activity, LocalDate occurrenceDate) {
        if (!isOccurrenceValid(activity, occurrenceDate)) {
            throw new BusinessException("La fecha seleccionada no corresponde a una clase programada");
        }
    }

    private ActivityRequest toSeriesRequest(Activity activity, ActivityOccurrenceEditRequest request) {
        return new ActivityRequest(
                activity.getName(),
                activity.getDescription(),
                activity.getStartDate(),
                activity.getEndDate(),
                request.startTime(),
                request.endTime(),
                request.locationName() != null ? request.locationName() : activity.getLocationName(),
                activity.getInstructor() != null ? activity.getInstructor().getId() : null,
                request.capacity() != null ? request.capacity() : activity.getCapacity(),
                activity.isRecurring(),
                ActivityRecurrenceUtil.parseRepeatDays(activity.getRepeatDays()),
                request.confirmAffectedReservations()
        );
    }

    private Map<Long, Map<LocalDate, ActivityOccurrenceOverride>> loadOverrides(
            List<Activity> series, LocalDate from, LocalDate to) {
        if (series.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = series.stream().map(Activity::getId).toList();
        List<ActivityOccurrenceOverride> overrides =
                overrideRepository.findByActivityIdsAndOccurrenceDateBetween(ids, from, to);
        Map<Long, Map<LocalDate, ActivityOccurrenceOverride>> result = new HashMap<>();
        for (ActivityOccurrenceOverride override : overrides) {
            result.computeIfAbsent(override.getActivity().getId(), k -> new HashMap<>())
                    .put(override.getOccurrenceDate(), override);
        }
        return result;
    }

    private Activity requireActivity(Long organizationId, Long id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada"));
        if (!activity.getOrganization().getId().equals(organizationId)) {
            throw new BusinessException("La actividad no pertenece a este gimnasio");
        }
        if (!activity.isActive()) {
            throw new BusinessException("La actividad ya no está activa");
        }
        return activity;
    }

    private Activity copySchedule(Activity source) {
        Activity copy = new Activity();
        copy.setStartDate(source.getStartDate());
        copy.setEndDate(source.getEndDate());
        copy.setRecurring(source.isRecurring());
        copy.setRepeatDays(source.getRepeatDays());
        return copy;
    }

    private List<Reservation> findActiveReservations(Long activityId) {
        return reservationRepository.findByActivityIdAndStatusInWithMember(activityId, ACTIVE_STATUSES);
    }

    private List<Reservation> findAffectedReservations(Long activityId, Activity newSchedule) {
        return findActiveReservations(activityId).stream()
                .filter(r -> !isOccurrenceValid(newSchedule, r.getOccurrenceDate()))
                .sorted(Comparator.comparing(Reservation::getOccurrenceDate))
                .toList();
    }

    private boolean isOccurrenceValid(Activity schedule, LocalDate occurrenceDate) {
        return !ActivityRecurrenceUtil.expandOccurrences(
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.isRecurring(),
                schedule.getRepeatDays(),
                occurrenceDate,
                occurrenceDate
        ).isEmpty();
    }

    private void cancelReservations(List<Reservation> reservations) {
        Instant now = Instant.now();
        for (Reservation reservation : reservations) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation.setUpdatedAt(now);
        }
        reservationRepository.saveAll(reservations);
    }

    private ActivityReservationImpactResponse toImpact(List<Reservation> active, List<Reservation> affected) {
        List<AffectedReservationItem> items = affected.stream()
                .map(r -> new AffectedReservationItem(
                        r.getId(),
                        r.getOccurrenceDate(),
                        r.getMember().getFirstName() + " " + r.getMember().getLastName(),
                        r.getStatus()
                ))
                .toList();
        return new ActivityReservationImpactResponse(active.size(), affected.size(), items);
    }

    private void applyRequest(Activity activity, ActivityRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("La fecha de fin no puede ser anterior a la de inicio");
        }

        activity.setName(request.name());
        activity.setDescription(request.description());
        activity.setStartDate(request.startDate());
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setLocationName(request.locationName());
        activity.setCapacity(request.capacity());

        if (request.recurring()) {
            if (request.repeatDays() == null || request.repeatDays().isEmpty()) {
                throw new BusinessException("Selecciona al menos un día de la semana para la recurrencia");
            }
            activity.setRecurring(true);
            activity.setEndDate(request.endDate());
            activity.setRepeatDays(ActivityRecurrenceUtil.serializeRepeatDays(request.repeatDays()));
        } else {
            activity.setRecurring(false);
            activity.setEndDate(request.startDate());
            activity.setRepeatDays(null);
        }

        if (request.instructorId() != null) {
            User instructor = userRepository.findById(request.instructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));
            activity.setInstructor(instructor);
        } else {
            activity.setInstructor(null);
        }
    }

    private ActivityResponse toSeriesResponse(Activity activity) {
        long confirmed = reservationRepository.countByActivityIdAndStatus(activity.getId(), ReservationStatus.CONFIRMED);
        long pending = reservationRepository.countByActivityIdAndStatus(activity.getId(), ReservationStatus.PENDING);
        return buildResponse(activity, activity.getStartDate(), confirmed, pending, null);
    }

    private ActivityResponse toOccurrenceResponse(
            Activity activity, LocalDate occurrenceDate, ActivityOccurrenceOverride override) {
        long confirmed = reservationRepository.countByActivityIdAndOccurrenceDateAndStatus(
                activity.getId(), occurrenceDate, ReservationStatus.CONFIRMED);
        long pending = reservationRepository.countByActivityIdAndOccurrenceDateAndStatus(
                activity.getId(), occurrenceDate, ReservationStatus.PENDING);
        return buildResponse(activity, occurrenceDate, confirmed, pending, override);
    }

    private ActivityResponse buildResponse(
            Activity activity,
            LocalDate displayDate,
            long confirmed,
            long pending,
            ActivityOccurrenceOverride override) {
        LocalTime startTime = override != null ? override.getStartTime() : activity.getStartTime();
        LocalTime endTime = override != null && override.getEndTime() != null
                ? override.getEndTime()
                : activity.getEndTime();
        String locationName = override != null && override.getLocationName() != null
                ? override.getLocationName()
                : activity.getLocationName();
        Integer capacity = override != null && override.getCapacity() != null
                ? override.getCapacity()
                : activity.getCapacity();

        boolean hasCapacity = capacity == null || (confirmed + pending) < capacity;
        String instructorName = activity.getInstructor() != null
                ? activity.getInstructor().getFirstName() + " " + activity.getInstructor().getLastName()
                : null;

        return new ActivityResponse(
                activity.getId(),
                activity.getName(),
                activity.getDescription(),
                displayDate,
                activity.getStartDate(),
                activity.getEndDate(),
                activity.isRecurring(),
                ActivityRecurrenceUtil.parseRepeatDays(activity.getRepeatDays()),
                startTime,
                endTime,
                locationName,
                activity.getInstructor() != null ? activity.getInstructor().getId() : null,
                instructorName,
                capacity,
                confirmed,
                pending,
                hasCapacity,
                override != null,
                activity.isActive(),
                activity.getCreatedAt()
        );
    }
}
