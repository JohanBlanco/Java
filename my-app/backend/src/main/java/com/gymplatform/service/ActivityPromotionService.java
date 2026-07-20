package com.gymplatform.service;

import com.gymplatform.domain.entity.Activity;
import com.gymplatform.domain.entity.ActivityPromotion;
import com.gymplatform.dto.ActivityPromotionRequest;
import com.gymplatform.dto.ActivityPromotionResponse;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.ActivityPromotionRepository;
import com.gymplatform.repository.ActivityRepository;
import com.gymplatform.repository.OrganizationRepository;
import com.gymplatform.repository.ReservationRepository;
import com.gymplatform.util.ActivityRecurrenceUtil;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityPromotionService {

    private static final int MAX_SLOTS = 3;

    private final ActivityPromotionRepository promotionRepository;
    private final ActivityRepository activityRepository;
    private final OrganizationRepository organizationRepository;
    private final ReservationRepository reservationRepository;

    public ActivityPromotionService(
            ActivityPromotionRepository promotionRepository,
            ActivityRepository activityRepository,
            OrganizationRepository organizationRepository,
            ReservationRepository reservationRepository) {
        this.promotionRepository = promotionRepository;
        this.activityRepository = activityRepository;
        this.organizationRepository = organizationRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityPromotionResponse> getAdminSlots(Long organizationId) {
        Map<Integer, ActivityPromotion> bySlot = new HashMap<>();
        promotionRepository.findByOrganizationIdOrderBySlotIndexAsc(organizationId)
                .forEach(p -> bySlot.put(p.getSlotIndex(), p));

        List<ActivityPromotionResponse> result = new ArrayList<>(MAX_SLOTS);
        for (int slot = 1; slot <= MAX_SLOTS; slot++) {
            ActivityPromotion promotion = bySlot.get(slot);
            result.add(promotion == null
                    ? emptySlot(slot)
                    : toResponse(promotion, true, reservationCount(promotion.getActivity().getId())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ActivityPromotionResponse> getHomePromotions(Long organizationId) {
        List<ActivityPromotion> manual =
                promotionRepository.findByOrganizationIdOrderBySlotIndexAsc(organizationId);
        if (!manual.isEmpty()) {
            return manual.stream()
                    .filter(p -> nextOccurrence(p.getActivity()) != null)
                    .map(p -> toResponse(p, true, reservationCount(p.getActivity().getId())))
                    .toList();
        }

        Map<Long, Long> counts = reservationCounts(organizationId);
        LocalDate today = LocalDate.now();
        return activityRepository
                .findByOrganizationIdAndActiveTrueOrderByStartDateAscStartTimeAsc(organizationId)
                .stream()
                .filter(a -> !a.getEndDate().isBefore(today))
                .filter(a -> nextOccurrence(a) != null)
                .sorted(Comparator
                        .<Activity>comparingLong(a -> counts.getOrDefault(a.getId(), 0L))
                        .reversed()
                        .thenComparing(Activity::getStartDate)
                        .thenComparing(Activity::getStartTime))
                .limit(MAX_SLOTS)
                .map(a -> toFallbackResponse(a, counts.getOrDefault(a.getId(), 0L)))
                .toList();
    }

    @Transactional
    public ActivityPromotionResponse saveSlot(
            Long organizationId,
            int slotIndex,
            ActivityPromotionRequest request) {
        assertSlot(slotIndex);
        Activity activity = activityRepository.findById(request.activityId())
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada"));
        if (!activity.getOrganization().getId().equals(organizationId)) {
            throw new BusinessException("La actividad no pertenece a este gimnasio");
        }
        if (!activity.isActive() || nextOccurrence(activity) == null) {
            throw new BusinessException("Solo puedes promocionar una actividad activa con fechas próximas");
        }
        if (promotionRepository.existsByOrganizationIdAndActivityIdAndSlotIndexNot(
                organizationId, activity.getId(), slotIndex)) {
            throw new BusinessException("Esta actividad ya está promocionada en otro espacio");
        }

        ActivityPromotion promotion = promotionRepository
                .findByOrganizationIdAndSlotIndex(organizationId, slotIndex)
                .orElseGet(ActivityPromotion::new);
        if (promotion.getOrganization() == null) {
            promotion.setOrganization(organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada")));
            promotion.setSlotIndex(slotIndex);
        }
        promotion.setActivity(activity);
        promotion.setImageUrl(normalizeImageUrl(request.imageUrl()));
        return toResponse(
                promotionRepository.save(promotion),
                true,
                reservationCount(activity.getId()));
    }

    @Transactional
    public void clearSlot(Long organizationId, int slotIndex) {
        assertSlot(slotIndex);
        promotionRepository.findByOrganizationIdAndSlotIndex(organizationId, slotIndex)
                .ifPresent(promotionRepository::delete);
    }

    private ActivityPromotionResponse toFallbackResponse(Activity activity, long count) {
        LocalDate occurrence = nextOccurrence(activity);
        return new ActivityPromotionResponse(
                0,
                true,
                false,
                activity.getId(),
                activity.getName(),
                activity.getDescription(),
                activity.getImageUrl(),
                occurrence,
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getLocationName(),
                instructorName(activity),
                count);
    }

    private ActivityPromotionResponse toResponse(
            ActivityPromotion promotion,
            boolean manual,
            long count) {
        Activity activity = promotion.getActivity();
        String imageUrl = promotion.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = activity.getImageUrl();
        }
        return new ActivityPromotionResponse(
                promotion.getSlotIndex(),
                true,
                manual,
                activity.getId(),
                activity.getName(),
                activity.getDescription(),
                imageUrl,
                nextOccurrence(activity),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getLocationName(),
                instructorName(activity),
                count);
    }

    private ActivityPromotionResponse emptySlot(int slot) {
        return new ActivityPromotionResponse(
                slot, false, true, null, null, null, null,
                null, null, null, null, null, 0);
    }

    private LocalDate nextOccurrence(Activity activity) {
        LocalDate today = LocalDate.now();
        if (!activity.isActive() || activity.getEndDate().isBefore(today)) {
            return null;
        }
        return ActivityRecurrenceUtil.expandOccurrences(
                        activity.getStartDate(),
                        activity.getEndDate(),
                        activity.isRecurring(),
                        activity.getRepeatDays(),
                        today,
                        activity.getEndDate())
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String instructorName(Activity activity) {
        return activity.getInstructor() == null
                ? null
                : activity.getInstructor().getFirstName() + " "
                        + activity.getInstructor().getLastName();
    }

    private long reservationCount(Long activityId) {
        return reservationRepository.countByActivityIdAndStatus(
                activityId,
                com.gymplatform.domain.enums.ReservationStatus.CONFIRMED);
    }

    private Map<Long, Long> reservationCounts(Long organizationId) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : reservationRepository.countConfirmedGroupedByActivity(organizationId)) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private void assertSlot(int slotIndex) {
        if (slotIndex < 1 || slotIndex > MAX_SLOTS) {
            throw new BusinessException("El espacio debe estar entre 1 y 3");
        }
    }

    private String normalizeImageUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("La URL de imagen no es válida");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new BusinessException("La imagen debe usar una URL http o https");
        }
        return value;
    }
}
