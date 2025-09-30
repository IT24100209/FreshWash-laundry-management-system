package org.example.laudarymanagement.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.laudarymanagement.model.Order;
import org.example.laudarymanagement.model.OrderStatusHistory;
import org.example.laudarymanagement.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) { this.orderService = orderService; }

    public record CreateRequest(String userEmail, String serviceType, String specialInstructions, String fulfillment) {}
    public record StatusRequest(String toStatus, String assignedStaffEmail) {}

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest req) {
        try {
            Order.ServiceType type = Order.ServiceType.valueOf(req.serviceType().toUpperCase());
            Order order = orderService.create(req.userEmail(), type, req.specialInstructions());
            // set fulfillment if provided
            if (req.fulfillment() != null) {
                try {
                    order.setFulfillment(Order.Fulfillment.valueOf(req.fulfillment().toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid service type"));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        Optional<Order> o = orderService.cancel(id);
        return o.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/repeat")
    public ResponseEntity<?> repeat(@PathVariable Long id) {
        Optional<Order> o = orderService.repeat(id);
        return o.<ResponseEntity<?>>map(ord -> ResponseEntity.status(HttpStatus.CREATED).body(ord))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        try {
            Order.Status to = Order.Status.valueOf(req.toStatus().toUpperCase());
            Optional<Order> o = orderService.updateStatus(id, to, req.assignedStaffEmail());
            return o.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid status"));
        }
    }

    @GetMapping
    public List<Order> listForUser(@RequestParam String email) { return orderService.listForUser(email); }

    @GetMapping("/staff")
    public List<Order> listForStaff(@RequestParam String email) { return orderService.listForStaff(email); }

    @GetMapping("/{id}/timeline")
    public List<OrderStatusHistory> timeline(@PathVariable Long id) { return orderService.timeline(id); }

    // Admin: list all orders
    @GetMapping("/all")
    public List<Order> listAll() { return orderService.listAll(); }
}


