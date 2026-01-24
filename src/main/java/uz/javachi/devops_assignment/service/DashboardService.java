package uz.javachi.devops_assignment.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.javachi.devops_assignment.model.DashboardStats;
import uz.javachi.devops_assignment.model.Order;
import uz.javachi.devops_assignment.model.Product;
import uz.javachi.devops_assignment.repository.OrderRepository;
import uz.javachi.devops_assignment.repository.ProductRepository;
import uz.javachi.devops_assignment.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final Counter dashboardRequestCounter;
    private final Counter dashboardErrorCounter;
    private final Timer dashboardQueryTimer;

    public DashboardService(ProductRepository productRepository,
                           OrderRepository orderRepository,
                           UserRepository userRepository,
                           MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        
        this.dashboardRequestCounter = Counter.builder("dashboard.requests.total")
                .description("Total number of dashboard API requests")
                .tag("api", "dashboard")
                .register(meterRegistry);
                
        this.dashboardErrorCounter = Counter.builder("dashboard.errors.total")
                .description("Total number of dashboard API errors")
                .tag("api", "dashboard")
                .register(meterRegistry);
                
        this.dashboardQueryTimer = Timer.builder("dashboard.database.query.time")
                .description("Time taken for dashboard database queries")
                .tag("operation", "query")
                .register(meterRegistry);
    }

    @Timed(value = "dashboard.service.getStats", description = "Time to fetch dashboard statistics")
    public DashboardStats getDashboardStats() {
        return dashboardQueryTimer.record(() -> {
            log.info("Getting dashboard statistics");
            
            DashboardStats stats = new DashboardStats();
            
            // Total products
            stats.setTotalProducts(productRepository.count());
            
            // Total orders
            stats.setTotalOrders(orderRepository.count());
            
            // Total users
            stats.setTotalUsers(userRepository.count());
            
            // Daily orders (orders created today)
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            List<Order> allOrders = orderRepository.findAll();
            long dailyOrders = allOrders.stream()
                    .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().isAfter(today))
                    .count();
            stats.setDailyOrders(dailyOrders);
            
            // Total revenue
            double totalRevenue = allOrders.stream()
                    .filter(order -> order.getTotalPrice() != null)
                    .mapToDouble(Order::getTotalPrice)
                    .sum();
            stats.setTotalRevenue(totalRevenue);
            
            // Popular products
            Map<Long, List<Order>> ordersByProduct = allOrders.stream()
                    .filter(order -> order.getProductId() != null)
                    .collect(Collectors.groupingBy(Order::getProductId));
            
            List<DashboardStats.PopularProduct> popularProducts = ordersByProduct.entrySet().stream()
                    .map(entry -> {
                        Long productId = entry.getKey();
                        List<Order> orders = entry.getValue();
                        
                        Product product = productRepository.findById(productId).orElse(null);
                        if (product == null) {
                            return null;
                        }
                        
                        DashboardStats.PopularProduct popularProduct = new DashboardStats.PopularProduct();
                        popularProduct.setProductId(productId);
                        popularProduct.setProductName(product.getName());
                        popularProduct.setOrderCount((long) orders.size());
                        popularProduct.setTotalRevenue(orders.stream()
                                .filter(order -> order.getTotalPrice() != null)
                                .mapToDouble(Order::getTotalPrice)
                                .sum());
                        return popularProduct;
                    })
                    .filter(product -> product != null)
                    .sorted((a, b) -> Long.compare(b.getOrderCount(), a.getOrderCount()))
                    .limit(10)
                    .collect(Collectors.toList());
            
            stats.setPopularProducts(popularProducts);
            
            return stats;
        });
    }

    @Timed(value = "dashboard.service.getRevenue", description = "Time to fetch revenue statistics")
    public Double getTotalRevenue() {
        return dashboardQueryTimer.record(() -> {
            log.info("Getting total revenue");
            List<Order> allOrders = orderRepository.findAll();
            return allOrders.stream()
                    .filter(order -> order.getTotalPrice() != null)
                    .mapToDouble(Order::getTotalPrice)
                    .sum();
        });
    }

    @Timed(value = "dashboard.service.getPopularProducts", description = "Time to fetch popular products")
    public List<DashboardStats.PopularProduct> getPopularProducts() {
        return dashboardQueryTimer.record(() -> {
            log.info("Getting popular products");
            List<Order> allOrders = orderRepository.findAll();
            
            Map<Long, List<Order>> ordersByProduct = allOrders.stream()
                    .filter(order -> order.getProductId() != null)
                    .collect(Collectors.groupingBy(Order::getProductId));
            
            return ordersByProduct.entrySet().stream()
                    .map(entry -> {
                        Long productId = entry.getKey();
                        List<Order> orders = entry.getValue();
                        
                        Product product = productRepository.findById(productId).orElse(null);
                        if (product == null) {
                            return null;
                        }
                        
                        DashboardStats.PopularProduct popularProduct = new DashboardStats.PopularProduct();
                        popularProduct.setProductId(productId);
                        popularProduct.setProductName(product.getName());
                        popularProduct.setOrderCount((long) orders.size());
                        popularProduct.setTotalRevenue(orders.stream()
                                .filter(order -> order.getTotalPrice() != null)
                                .mapToDouble(Order::getTotalPrice)
                                .sum());
                        return popularProduct;
                    })
                    .filter(product -> product != null)
                    .sorted((a, b) -> Long.compare(b.getOrderCount(), a.getOrderCount()))
                    .limit(10)
                    .collect(Collectors.toList());
        });
    }
}
