package com.quickmove.controller;

// Touch file to refresh IDE indexing - no-op change

import com.quickmove.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<com.quickmove.entity.Payment> createPayment(@RequestBody com.quickmove.entity.Payment payment) {
        com.quickmove.entity.Payment savedPayment = paymentService.savePayment(payment);
        return ResponseEntity.ok(savedPayment);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<com.quickmove.entity.Payment> updatePaymentStatus(@PathVariable Long id, @RequestBody String status) {
        com.quickmove.entity.Payment updatedPayment = paymentService.updatePaymentStatus(id, status);
        return ResponseEntity.ok(updatedPayment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.quickmove.entity.Payment> getPaymentById(@PathVariable Long id) {
        com.quickmove.entity.Payment payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/ride/{rideId}")
    public ResponseEntity<com.quickmove.entity.Payment> getPaymentByRideId(@PathVariable Long rideId) {
        java.util.Optional<com.quickmove.entity.Payment> p = paymentService.findByRideId(rideId);
        return p.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<com.quickmove.entity.Payment>> getAllPayments() {
        List<com.quickmove.entity.Payment> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<com.quickmove.entity.Payment> confirmPayment(@PathVariable Long id, @RequestBody com.quickmove.entity.Payment confirmationDetails) {
        com.quickmove.entity.Payment confirmedPayment = paymentService.confirmPayment(id, confirmationDetails);
        return ResponseEntity.ok(confirmedPayment);
    }
}