package org.example.laudarymanagement.service;

import java.util.List;
import java.util.Optional;
import org.example.laudarymanagement.model.Order;
import org.example.laudarymanagement.model.OrderStatusHistory;
import org.example.laudarymanagement.repository.OrderRepository;
import org.example.laudarymanagement.repository.OrderStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;

    public OrderService(OrderRepository orderRepository, OrderStatusHistoryRepository historyRepository) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public Order create(String userEmail, Order.ServiceType serviceType, String specialInstructions) {
        Order order = new Order();
        order.setUserEmail(userEmail);
        order.setServiceType(serviceType);
        order.setSpecialInstructions(specialInstructions);
        order.setStatus(Order.Status.PICKUP);
        Order saved = orderRepository.save(order);
        recordHistory(saved.getId(), null, Order.Status.PICKUP);
        return saved;
    }

    @Transactional
    public Optional<Order> cancel(Long id) {
        return orderRepository.findById(id).map(o -> {
            if (o.getStatus() == Order.Status.COMPLETED || o.getStatus() == Order.Status.CANCELLED) return o;
            Order.Status from = o.getStatus();
            o.setStatus(Order.Status.CANCELLED);
            Order saved = orderRepository.save(o);
            recordHistory(saved.getId(), from, Order.Status.CANCELLED);
            return saved;
        });
    }

    @Transactional
    public Optional<Order> repeat(Long id) {
        return orderRepository.findById(id).map(o -> create(o.getUserEmail(), o.getServiceType(), o.getSpecialInstructions()));
    }

    @Transactional
    public Optional<Order> updateStatus(Long id, Order.Status toStatus, String assignedStaffEmail) {
        return orderRepository.findById(id).map(o -> {
            Order.Status from = o.getStatus();
            o.setStatus(toStatus);
            if (assignedStaffEmail != null) o.setAssignedStaffEmail(assignedStaffEmail);
            Order saved = orderRepository.save(o);
            recordHistory(saved.getId(), from, toStatus);
            return saved;
        });
    }

    public List<Order> listForUser(String email) { return orderRepository.findByUserEmailOrderByCreatedAtDesc(email); }
    public List<Order> listForStaff(String email) { return orderRepository.findByAssignedStaffEmailOrderByCreatedAtDesc(email); }
    public List<OrderStatusHistory> timeline(Long orderId) { return historyRepository.findByOrderIdOrderByChangedAtAsc(orderId); }
    public List<Order> listAll() { return orderRepository.findAll(); }

    private void recordHistory(Long orderId, Order.Status from, Order.Status to) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(orderId);
        h.setFromStatus(from);
        h.setToStatus(to);
        historyRepository.save(h);
    }
}


