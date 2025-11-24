package com.quickmove.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "drivers")
@Data
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String userId;
    private String vehicleType; // bike, car, auto
    private String numberPlate;
//    private String phone; // Driver's phone number
    private String status; // AVAILABLE, BUSY, OFFLINE

    // Approval state for driver onboarding (PENDING -> false, APPROVED -> true)
    private Boolean approved = false;
}