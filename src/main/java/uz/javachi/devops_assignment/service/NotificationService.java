package uz.javachi.devops_assignment.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.javachi.devops_assignment.model.Notification;
import uz.javachi.devops_assignment.model.NotificationType;
import uz.javachi.devops_assignment.model.Order;
import uz.javachi.devops_assignment.model.Product;
import uz.javachi.devops_assignment.repository.NotificationRepository;
import uz.javachi.devops_assignment.repository.ProductRepository;
import uz.javachi.devops_assignment.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final Counter notificationSentCounter;
    private final Counter notificationErrorCounter;
    private final Timer notificationQueryTimer;

    public NotificationService(NotificationRepository notificationRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository,
                              EmailService emailService,
                              MeterRegistry meterRegistry) {
        this.notificationRepository = notificationRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        
        this.notificationSentCounter = Counter.builder("notifications.sent.total")
                .description("Total number of notifications sent")
                .tag("operation", "send")
                .register(meterRegistry);
                
        this.notificationErrorCounter = Counter.builder("notifications.errors.total")
                .description("Total number of notification errors")
                .tag("api", "notifications")
                .register(meterRegistry);
                
        this.notificationQueryTimer = Timer.builder("notifications.database.query.time")
                .description("Time taken for notification database queries")
                .tag("operation", "query")
                .register(meterRegistry);
    }

    @Timed(value = "notifications.service.sendOrderNotification", description = "Time to send order notification")
    public void sendOrderNotification(Order order, Product product) {
        try {
            log.info("Sending order notification for order: {}", order.getId());
            
            // Get farmer ID from product
            String farmerId = product.getFarmerId();
            
            // Create notification message
            String message = String.format(
                "Yangi buyurtma qabul qilindi! Mahsulot: %s, Miqdor: %d, Jami narx: %.2f",
                product.getName(), order.getQuantity(), order.getTotalPrice()
            );
            
            // Save notification to database
            Notification notification = new Notification();
            notification.setUserId(farmerId);
            notification.setOrderId(order.getId());
            notification.setMessage(message);
            notification.setType(NotificationType.ORDER_CREATED);
            notificationRepository.save(notification);
            
            // Send email notification
            String emailSubject = "Yangi buyurtma qabul qilindi";
            String emailBody = String.format(
                "Hurmatli fermer,\n\n" +
                "Sizning mahsulotingizga yangi buyurtma qabul qilindi:\n" +
                "Mahsulot: %s\n" +
                "Miqdor: %d\n" +
                "Jami narx: %.2f\n" +
                "Buyurtma ID: %d\n\n" +
                "Iltimos, buyurtmani tekshiring va tasdiqlang.",
                product.getName(), order.getQuantity(), order.getTotalPrice(), order.getId()
            );
            
            // Get user email (mock - in real app would get from user entity)
            String userEmail = "farmer-" + farmerId + "@example.com";
            emailService.sendEmail(userEmail, emailSubject, emailBody);
            
            notificationSentCounter.increment();
            log.info("Order notification sent successfully");
        } catch (Exception e) {
            log.error("Error sending order notification", e);
            notificationErrorCounter.increment();
        }
    }

    @Timed(value = "notifications.service.sendPriceUpdateNotification", description = "Time to send price update notification")
    public void sendPriceUpdateNotification(Product product, Double oldPrice) {
        try {
            log.info("Sending price update notification for product: {}", product.getId());
            
            // Get farmer ID
            String farmerId = product.getFarmerId();
            
            // Create notification message
            String message = String.format(
                "Mahsulot narxi yangilandi: %s, Eski narx: %.2f, Yangi narx: %.2f",
                product.getName(), oldPrice, product.getPrice()
            );
            
            // Save notification
            Notification notification = new Notification();
            notification.setUserId(farmerId);
            notification.setMessage(message);
            notification.setType(NotificationType.PRICE_UPDATED);
            notificationRepository.save(notification);
            
            // Send email
            String emailSubject = "Mahsulot narxi yangilandi";
            String emailBody = String.format(
                "Hurmatli fermer,\n\n" +
                "Sizning mahsulotingizning narxi yangilandi:\n" +
                "Mahsulot: %s\n" +
                "Eski narx: %.2f\n" +
                "Yangi narx: %.2f\n",
                product.getName(), oldPrice, product.getPrice()
            );
            
            String userEmail = "farmer-" + farmerId + "@example.com";
            emailService.sendEmail(userEmail, emailSubject, emailBody);
            
            notificationSentCounter.increment();
        } catch (Exception e) {
            log.error("Error sending price update notification", e);
            notificationErrorCounter.increment();
        }
    }

    @Timed(value = "notifications.service.getByUser", description = "Time to fetch notifications by user")
    public List<Notification> getNotificationsByUser(String userId) {
        return notificationQueryTimer.record(() -> {
            log.info("Getting notifications for user: {}", userId);
            return notificationRepository.findByUserId(userId);
        });
    }

    @Timed(value = "notifications.service.markAsRead", description = "Time to mark notification as read")
    public void markAsRead(Long notificationId) {
        notificationQueryTimer.record(() -> {
            log.info("Marking notification as read: {}", notificationId);
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
}
