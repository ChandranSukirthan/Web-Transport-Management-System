package com.quickmove.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_sessions")
@Data
public class TrackingSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    // Rider is represented by userId string in Ride entity
    @Column(name = "rider_user_id", nullable = false)
    private String riderUserId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(length = 20)
    private String status = "ACTIVE"; // ACTIVE, ENDED, CANCELLED

    @Column(name = "created_by", length = 32)
    private String createdBy; // rider, driver, system

    // acceptance flags for each party
    @Column(name = "driver_accepted")
    private Boolean driverAccepted = false;

    @Column(name = "rider_accepted")
    private Boolean riderAccepted = false;

    // Explicit accessors to satisfy references without relying on Lombok processing in some tools
    public Boolean getRiderAccepted() {
        return riderAccepted;
    }
    public void setRiderAccepted(Boolean riderAccepted) {
        this.riderAccepted = riderAccepted;
    }

    public Boolean getDriverAccepted() {
        return driverAccepted;
    }
    public void setDriverAccepted(Boolean driverAccepted) {
        this.driverAccepted = driverAccepted;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Ride getRide() {
        return ride;
    }
    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public Driver getDriver() {
        return driver;
    }
    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public String getRiderUserId() {
        return riderUserId;
    }
    public void setRiderUserId(String riderUserId) {
        this.riderUserId = riderUserId;
    }

    public String getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}
