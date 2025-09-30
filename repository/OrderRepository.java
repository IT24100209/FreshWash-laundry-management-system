package org.example.laudarymanagement.repository;

import java.util.List;
import org.example.laudarymanagement.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    List<Order> findByAssignedStaffEmailOrderByCreatedAtDesc(String assignedStaffEmail);
}





