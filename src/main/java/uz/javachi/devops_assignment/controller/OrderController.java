package uz.javachi.devops_assignment.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.javachi.devops_assignment.model.Order;
import uz.javachi.devops_assignment.model.OrderStatus;
import uz.javachi.devops_assignment.service.OrderService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final Counter orderRequestCounter;
    private final Counter orderErrorCounter;

    public OrderController(OrderService orderService, MeterRegistry meterRegistry) {
        this.orderService = orderService;
        
        this.orderRequestCounter = Counter.builder("orders.requests.total")
                .description("Total number of order API requests")
                .tag("api", "orders")
                .register(meterRegistry);
                
        this.orderErrorCounter = Counter.builder("orders.errors.total")
                .description("Total number of order API errors")
                .tag("api", "orders")
                .register(meterRegistry);
    }

    @GetMapping
    @Timed(value = "orders.get.all", description = "Time taken to get all orders")
    public ResponseEntity<?> getAllOrders() {
        log.info("Get all orders endpoint called");
        orderRequestCounter.increment();
        
        try {
            List<Order> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error getting orders", e);
            orderErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Timed(value = "orders.get.byid", description = "Time taken to get order by ID")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        log.info("Get order by id endpoint called: {}", id);
        orderRequestCounter.increment();
        
        try {
            Order order = orderService.getOrderById(id);
            if (order != null) {
                return ResponseEntity.ok(order);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting order by id", e);
            orderErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    @Timed(value = "orders.create", description = "Time taken to create order")
    public ResponseEntity<?> createOrder(@Valid @RequestBody Order order) {
        log.info("Create order endpoint called");
        orderRequestCounter.increment();
        
        try {
            Order created = orderService.createOrder(order);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating order", e);
            orderErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @Timed(value = "orders.update.status", description = "Time taken to update order status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        log.info("Update order status endpoint called: {}", id);
        orderRequestCounter.increment();
        
        try {
            OrderStatus status = OrderStatus.valueOf(request.getStatus().toUpperCase());
            Order updated = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating order status", e);
            orderErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // DTO for status update request
    public static class StatusUpdateRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
