package uz.javachi.devops_assignment.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.javachi.devops_assignment.model.Order;
import uz.javachi.devops_assignment.model.OrderStatus;
import uz.javachi.devops_assignment.model.Product;
import uz.javachi.devops_assignment.repository.OrderRepository;
import uz.javachi.devops_assignment.repository.ProductRepository;

import java.util.List;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final Counter orderCreateCounter;
    private final Counter orderUpdateCounter;
    private final Counter orderErrorCounter;
    private final Timer orderQueryTimer;

    public OrderService(OrderRepository orderRepository, 
                       ProductRepository productRepository,
                       NotificationService notificationService,
                       MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        
        this.orderCreateCounter = Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .tag("operation", "create")
                .register(meterRegistry);
                
        this.orderUpdateCounter = Counter.builder("orders.updated.total")
                .description("Total number of orders updated")
                .tag("operation", "update")
                .register(meterRegistry);
                
        this.orderErrorCounter = Counter.builder("orders.errors.total")
                .description("Total number of order operation errors")
                .tag("api", "orders")
                .register(meterRegistry);
                
        this.orderQueryTimer = Timer.builder("orders.database.query.time")
                .description("Time taken for order database queries")
                .tag("operation", "query")
                .register(meterRegistry);
    }

    @Timed(value = "orders.service.getAll", description = "Time to fetch all orders")
    public List<Order> getAllOrders() {
        return orderQueryTimer.record(() -> {
            log.info("Getting all orders");
            return orderRepository.findAll();
        });
    }

    @Timed(value = "orders.service.getById", description = "Time to fetch order by ID")
    public Order getOrderById(Long id) {
        return orderQueryTimer.record(() -> {
            log.info("Getting order by id: {}", id);
            return orderRepository.findById(id).orElse(null);
        });
    }

    @Timed(value = "orders.service.create", description = "Time to create order")
    public Order createOrder(Order order) {
        return orderQueryTimer.record(() -> {
            log.info("Creating new order for product: {}", order.getProductId());
            
            // Validate product exists and has enough quantity
            Product product = productRepository.findById(order.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + order.getProductId()));
            
            if (product.getQuantity() < order.getQuantity()) {
                throw new RuntimeException("Insufficient product quantity. Available: " + product.getQuantity());
            }
            
            // Calculate total price
            order.setTotalPrice(product.getPrice() * order.getQuantity());
            order.setStatus(OrderStatus.PENDING);
            
            // Update product quantity
            product.setQuantity(product.getQuantity() - order.getQuantity());
            productRepository.save(product);
            
            Order saved = orderRepository.save(order);
            orderCreateCounter.increment();
            
            // Send notification to farmer
            notificationService.sendOrderNotification(saved, product);
            
            return saved;
        });
    }

    @Timed(value = "orders.service.updateStatus", description = "Time to update order status")
    public Order updateOrderStatus(Long id, OrderStatus status) {
        return orderQueryTimer.record(() -> {
            log.info("Updating order status: {} to {}", id, status);
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
            
            order.setStatus(status);
            Order updated = orderRepository.save(order);
            orderUpdateCounter.increment();
            
            return updated;
        });
    }

    @Timed(value = "orders.service.getByBuyer", description = "Time to fetch orders by buyer")
    public List<Order> getOrdersByBuyer(String buyerId) {
        return orderQueryTimer.record(() -> {
            log.info("Getting orders for buyer: {}", buyerId);
            return orderRepository.findByBuyerId(buyerId);
        });
    }
}
