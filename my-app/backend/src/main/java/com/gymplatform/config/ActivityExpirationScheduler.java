package com.gymplatform.config;

import com.gymplatform.service.ActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ActivityExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ActivityExpirationScheduler.class);

    private final ActivityService activityService;

    public ActivityExpirationScheduler(ActivityService activityService) {
        this.activityService = activityService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void purgeExpiredOnStartup() {
        int count = activityService.purgeAllExpiredActivities();
        if (count > 0) {
            log.info("Al iniciar: {} actividades vencidas eliminadas de la base de datos", count);
        }
    }

    /** Elimina actividades cuya fecha de fin ya pasó (vigencia vencida). */
    @Scheduled(cron = "0 0 1 * * *")
    public void purgeExpiredActivitiesDaily() {
        int count = activityService.purgeAllExpiredActivities();
        if (count > 0) {
            log.info("Actividades vencidas eliminadas automáticamente: {}", count);
        }
    }
}
