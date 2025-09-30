package org.example.laudarymanagement.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.laudarymanagement.model.Order;
import org.example.laudarymanagement.model.Schedule;
import org.example.laudarymanagement.repository.OrderRepository;
import org.example.laudarymanagement.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final OrderRepository orderRepository;

    public ScheduleService(ScheduleRepository scheduleRepository, OrderRepository orderRepository) {
        this.scheduleRepository = scheduleRepository;
        this.orderRepository = orderRepository;
    }

    public boolean hasConflict(String staffEmail, LocalDateTime start, LocalDateTime end) {
        if (staffEmail == null || staffEmail.isBlank()) return false;
        return !scheduleRepository.findConflicts(staffEmail, start, end).isEmpty();
    }

    @Transactional
    public Optional<Schedule> create(Long orderId, Schedule.Type type, LocalDateTime start, LocalDateTime end, String staffEmail, String routeNotes) {
        if (hasConflict(staffEmail, start, end)) return Optional.empty();
        Schedule s = new Schedule();
        s.setOrderId(orderId);
        s.setType(type);
        s.setSlotStart(start);
        s.setSlotEnd(end);
        s.setAssignedStaffEmail(staffEmail);
        s.setRouteNotes(routeNotes);
        s = scheduleRepository.save(s);
        return Optional.of(s);
    }

    public List<Schedule> tasksForStaff(String email) { return scheduleRepository.findByAssignedStaffEmailOrderBySlotStartAsc(email); }
    public List<Schedule> forOrder(Long orderId) { return scheduleRepository.findByOrderId(orderId); }
    public List<Schedule> listAll() { return scheduleRepository.findAll(); }

    public List<Schedule> listDeliverable() {
        List<Schedule> all = scheduleRepository.findAll();
        return all.stream()
                .filter(s -> s.getType() == Schedule.Type.DROPOFF)
                .filter(s -> {
                    return orderRepository.findById(s.getOrderId()).map(o ->
                            o.getStatus() == Order.Status.COMPLETED && o.getFulfillment() == Order.Fulfillment.DELIVERY
                    ).orElse(false);
                })
                .toList();
    }

    public List<Schedule> staffDeliverable(String email) {
        List<Schedule> byStaff = scheduleRepository.findByAssignedStaffEmailOrderBySlotStartAsc(email);
        return byStaff.stream()
                .filter(s -> s.getType() == Schedule.Type.DROPOFF)
                .filter(s -> {
                    return orderRepository.findById(s.getOrderId()).map(o ->
                            o.getStatus() == Order.Status.COMPLETED && o.getFulfillment() == Order.Fulfillment.DELIVERY
                    ).orElse(false);
                })
                .toList();
    }

    @Transactional
    public Optional<Schedule> updateStatus(Long id, Schedule.Status status, String routeNotes) {
        return scheduleRepository.findById(id).map(s -> {
            s.setStatus(status);
            if (routeNotes != null) s.setRouteNotes(routeNotes);
            return scheduleRepository.save(s);
        });
    }

    @Transactional
    public Optional<Schedule> assignToStaff(Long id, String staffEmail) {
        return scheduleRepository.findById(id).map(s -> {
            s.setAssignedStaffEmail(staffEmail);
            return scheduleRepository.save(s);
        });
    }
}


