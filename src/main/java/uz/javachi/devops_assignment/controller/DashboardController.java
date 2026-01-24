package uz.javachi.devops_assignment.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.javachi.devops_assignment.model.DashboardStats;
import uz.javachi.devops_assignment.service.DashboardService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final Counter dashboardRequestCounter;
    private final Counter dashboardErrorCounter;

    public DashboardController(DashboardService dashboardService, MeterRegistry meterRegistry) {
        this.dashboardService = dashboardService;
        
        this.dashboardRequestCounter = Counter.builder("dashboard.requests.total")
                .description("Total number of dashboard API requests")
                .tag("api", "dashboard")
                .register(meterRegistry);
                
        this.dashboardErrorCounter = Counter.builder("dashboard.errors.total")
                .description("Total number of dashboard API errors")
                .tag("api", "dashboard")
                .register(meterRegistry);
    }

    @GetMapping("/stats")
    @Timed(value = "dashboard.get.stats", description = "Time taken to get dashboard statistics")
    public ResponseEntity<?> getDashboardStats() {
        log.info("Get dashboard stats endpoint called");
        dashboardRequestCounter.increment();
        
        try {
            DashboardStats stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting dashboard stats", e);
            dashboardErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/revenue")
    @Timed(value = "dashboard.get.revenue", description = "Time taken to get revenue statistics")
    public ResponseEntity<?> getRevenue() {
        log.info("Get revenue endpoint called");
        dashboardRequestCounter.increment();
        
        try {
            Double revenue = dashboardService.getTotalRevenue();
            return ResponseEntity.ok(new RevenueResponse(revenue));
        } catch (Exception e) {
            log.error("Error getting revenue", e);
            dashboardErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/products/popular")
    @Timed(value = "dashboard.get.popular", description = "Time taken to get popular products")
    public ResponseEntity<?> getPopularProducts() {
        log.info("Get popular products endpoint called");
        dashboardRequestCounter.increment();
        
        try {
            List<DashboardStats.PopularProduct> products = dashboardService.getPopularProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting popular products", e);
            dashboardErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // DTO for revenue response
    public static class RevenueResponse {
        private Double totalRevenue;

        public RevenueResponse(Double totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public Double getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(Double totalRevenue) {
            this.totalRevenue = totalRevenue;
        }
    }
}
