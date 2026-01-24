package uz.javachi.devops_assignment.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    
    @NotNull(message = "Product ID is required")
    @Column(name = "product_id")
    private Long productId;
    
    @NotBlank(message = "Buyer ID is required")
    @Column(name = "buyer_id")
    private String buyerId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
    
    @Column(name = "total_price")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Double totalPrice;
    
    @Enumerated(EnumType.STRING)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OrderStatus status;
    
    @Column(name = "created_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        // Always set createdAt to current time (ignore any value from POST request)
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }
}
