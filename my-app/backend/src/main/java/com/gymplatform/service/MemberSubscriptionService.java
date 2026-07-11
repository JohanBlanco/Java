package com.gymplatform.service;

import com.gymplatform.domain.entity.MemberSubscription;
import com.gymplatform.domain.entity.MembershipPackage;
import com.gymplatform.domain.entity.User;
import com.gymplatform.domain.enums.ReservationStatus;
import com.gymplatform.dto.MembershipUsageResponse;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.MemberSubscriptionRepository;
import com.gymplatform.repository.MembershipPackageRepository;
import com.gymplatform.repository.ReservationRepository;
import com.gymplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class MemberSubscriptionService {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final MembershipPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public MemberSubscriptionService(MemberSubscriptionRepository subscriptionRepository,
                                       MembershipPackageRepository packageRepository,
                                       UserRepository userRepository,
                                       ReservationRepository reservationRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.packageRepository = packageRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public MemberSubscription assignMembership(Long organizationId, Long memberId, Long packageId) {
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado"));
        if (member.getOrganization() == null || !member.getOrganization().getId().equals(organizationId)) {
            throw new BusinessException("El miembro no pertenece a este gimnasio");
        }

        MembershipPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada"));
        if (!pkg.getOrganization().getId().equals(organizationId)) {
            throw new BusinessException("La membresía no pertenece a este gimnasio");
        }

        subscriptionRepository.findFirstByMemberIdAndActiveTrueOrderByStartDateDesc(memberId)
                .ifPresent(existing -> existing.setActive(false));

        MemberSubscription subscription = new MemberSubscription();
        subscription.setMember(member);
        subscription.setMembershipPackage(pkg);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(pkg.getDurationMonths()));
        subscription.setActive(true);
        return subscriptionRepository.save(subscription);
    }

    public Optional<MemberSubscription> getActiveSubscription(Long memberId) {
        return subscriptionRepository.findFirstByMemberIdAndActiveTrueOrderByStartDateDesc(memberId);
    }

    public long countUsedFreeActivities(Long memberId, MemberSubscription subscription) {
        Instant since = subscription.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        return reservationRepository.countByMemberIdAndFreeSlotTrueAndStatusAndCreatedAtGreaterThanEqual(
                memberId, ReservationStatus.CONFIRMED, since);
    }

    public boolean hasFreeSlotRemaining(Long memberId) {
        return getActiveSubscription(memberId)
                .map(sub -> {
                    Integer quota = sub.getMembershipPackage().getFreeActivityQuota();
                    if (quota == null) return true;
                    return countUsedFreeActivities(memberId, sub) < quota;
                })
                .orElse(false);
    }

    public MembershipUsageResponse getUsage(Long memberId) {
        Optional<MemberSubscription> subOpt = getActiveSubscription(memberId);
        if (subOpt.isEmpty()) {
            return new MembershipUsageResponse(null, null, null, 0, 0L, false);
        }
        MemberSubscription sub = subOpt.get();
        MembershipPackage pkg = sub.getMembershipPackage();
        Integer quota = pkg.getFreeActivityQuota();
        long used = countUsedFreeActivities(memberId, sub);
        boolean unlimited = quota == null;
        Long remaining = unlimited ? null : Math.max(0, quota - used);
        return new MembershipUsageResponse(
                pkg.getId(), pkg.getName(), quota, used, remaining, unlimited
        );
    }
}
