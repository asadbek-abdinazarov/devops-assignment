package uz.javachi.devops_assignment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "order_id")
    private Long orderId;
    
    private String message;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    private Boolean read = false;
    
    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        if (read == null) {
            read = false;
        }
    }
}
