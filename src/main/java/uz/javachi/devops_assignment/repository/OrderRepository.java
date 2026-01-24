package uz.javachi.devops_assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.javachi.devops_assignment.model.Order;
import uz.javachi.devops_assignment.model.OrderStatus;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerId(String buyerId);
    List<Order> findByProductId(Long productId);
    List<Order> findByStatus(OrderStatus status);
}
