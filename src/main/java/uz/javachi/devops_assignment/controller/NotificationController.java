package uz.javachi.devops_assignment.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.javachi.devops_assignment.model.Notification;
import uz.javachi.devops_assignment.service.NotificationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final Counter notificationRequestCounter;
    private final Counter notificationErrorCounter;

    public NotificationController(NotificationService notificationService, MeterRegistry meterRegistry) {
        this.notificationService = notificationService;
        
        this.notificationRequestCounter = Counter.builder("notifications.requests.total")
                .description("Total number of notification API requests")
                .tag("api", "notifications")
                .register(meterRegistry);
                
        this.notificationErrorCounter = Counter.builder("notifications.errors.total")
                .description("Total number of notification API errors")
                .tag("api", "notifications")
                .register(meterRegistry);
    }

    @GetMapping("/user/{userId}")
    @Timed(value = "notifications.get.byuser", description = "Time taken to get notifications by user")
    public ResponseEntity<?> getNotificationsByUser(@PathVariable String userId) {
        log.info("Get notifications by user endpoint called: {}", userId);
        notificationRequestCounter.increment();
        
        try {
            List<Notification> notifications = notificationService.getNotificationsByUser(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Error getting notifications", e);
            notificationErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/read")
    @Timed(value = "notifications.mark.read", description = "Time taken to mark notification as read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        log.info("Mark notification as read endpoint called: {}", id);
        notificationRequestCounter.increment();
        
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking notification as read", e);
            notificationErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
