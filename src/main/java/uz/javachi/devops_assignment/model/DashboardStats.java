package uz.javachi.devops_assignment.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DashboardStats {
    private Long totalProducts;
    private Long totalOrders;
    private Long totalUsers;
    private Long dailyOrders;
    private Double totalRevenue;
    private List<PopularProduct> popularProducts;
    
    @Getter
    @Setter
    public static class PopularProduct {
        private Long productId;
        private String productName;
        private Long orderCount;
        private Double totalRevenue;
    }
}
