package com.quickmove.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "rides")
@Data
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId; // Reference to User
    private String vehicleType; // bike, car, auto
    private String pickupLocation;
    private String dropoffLocation;
    private Double fare;
    private Double distance; // in km
    private Double duration; // in minutes
    private String status; // PENDING, BOOKED, CANCELLED, COMPLETED
    private String driverId; // Optional, assigned after booking
}