package org.example.laudarymanagement.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.laudarymanagement.model.Invoice;
import org.example.laudarymanagement.model.Order;
import org.example.laudarymanagement.model.Payment;
import org.example.laudarymanagement.model.Price;
import org.example.laudarymanagement.model.TransactionLog;
import org.example.laudarymanagement.repository.InvoiceRepository;
import org.example.laudarymanagement.repository.PaymentRepository;
import org.example.laudarymanagement.repository.PriceRepository;
import org.example.laudarymanagement.repository.TransactionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private final PriceRepository priceRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionLogRepository logRepository;

    public BillingService(PriceRepository priceRepository,
                          InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository,
                          TransactionLogRepository logRepository) {
        this.priceRepository = priceRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.logRepository = logRepository;
    }

    // Pricing
    public Price setPrice(Order.ServiceType type, BigDecimal amount) {
        Price price = priceRepository.findByServiceType(type).orElseGet(Price::new);
        price.setServiceType(type);
        price.setAmount(amount);
        return priceRepository.save(price);
    }

    public List<Price> listPrices() { return priceRepository.findAll(); }

    // Invoices
    @Transactional
    public Invoice generateInvoiceForOrder(Order order) {
        Optional<Invoice> existing = invoiceRepository.findByOrderId(order.getId());
        if (existing.isPresent()) return existing.get();
        BigDecimal total = priceRepository.findByServiceType(order.getServiceType())
                .map(Price::getAmount).orElse(new BigDecimal("0.00"));
        Invoice inv = new Invoice();
        inv.setOrderId(order.getId());
        inv.setUserEmail(order.getUserEmail());
        inv.setTotal(total);
        inv = invoiceRepository.save(inv);
        log("invoice:" + inv.getId(), "INVOICE_CREATED", inv.getTotal());
        return inv;
    }

    public Optional<Invoice> getInvoiceByOrder(Long orderId) { return invoiceRepository.findByOrderId(orderId); }
    public List<Invoice> userInvoices(String email) { return invoiceRepository.findByUserEmailOrderByCreatedAtDesc(email); }
    public List<Invoice> outstandingInvoices() { return invoiceRepository.findByStatusOrderByCreatedAtAsc(Invoice.Status.UNPAID); }
    public List<Invoice> listInvoicesByStatus(Invoice.Status status) {
        if (status == null) {
            return invoiceRepository.findAll();
        }
        return invoiceRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    // Payments
    @Transactional
    public Payment recordPayment(Long invoiceId, Payment.Method method, BigDecimal amount) {
        Payment p = new Payment();
        p.setInvoiceId(invoiceId);
        p.setMethod(method);
        p.setAmount(amount);
        p = paymentRepository.save(p);
        log("payment:" + p.getId(), "PAYMENT_RECEIVED", amount);

        // Mark invoice paid if fully covered
        Invoice inv = invoiceRepository.findById(invoiceId).orElseThrow();
        if (amount.compareTo(inv.getTotal()) >= 0) {
            inv.setStatus(Invoice.Status.PAID);
            invoiceRepository.save(inv);
        }
        return p;
    }

    public List<Payment> paymentsForInvoice(Long invoiceId) { return paymentRepository.findByInvoiceIdOrderByPaidAtDesc(invoiceId); }

    // Reports (simple totals)
    public Map<String, Object> summaryReport() {
        Map<String, Object> map = new HashMap<>();
        BigDecimal totalInvoiced = invoiceRepository.findAll().stream()
                .map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = paymentRepository.findAll().stream()
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        map.put("totalInvoiced", totalInvoiced);
        map.put("totalPaid", totalPaid);
        map.put("outstanding", totalInvoiced.subtract(totalPaid));
        return map;
    }

    private void log(String reference, String type, BigDecimal amount) {
        TransactionLog t = new TransactionLog();
        t.setReference(reference);
        t.setType(type);
        t.setAmount(amount);
        logRepository.save(t);
    }
}


