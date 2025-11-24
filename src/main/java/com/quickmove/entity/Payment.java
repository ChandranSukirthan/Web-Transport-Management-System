package com.quickmove.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long rideId;
    private String userId;
    private String driverId;
    private String vehicleType;
    private Double amount;
    private String method; // "MANUAL" or "CARD"
    private String status; // "PENDING", "SUCCESSFUL", "FAILED", "REJECTED"
    private String cardNumber; // Optional, for CARD
    private String cvc; // Optional, for CARD
}