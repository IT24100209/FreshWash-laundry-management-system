package org.example.laudarymanagement.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private Order.Status fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private Order.Status toStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Order.Status getFromStatus() { return fromStatus; }
    public void setFromStatus(Order.Status fromStatus) { this.fromStatus = fromStatus; }
    public Order.Status getToStatus() { return toStatus; }
    public void setToStatus(Order.Status toStatus) { this.toStatus = toStatus; }
    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
}





