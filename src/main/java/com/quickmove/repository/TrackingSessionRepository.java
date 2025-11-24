package com.quickmove.repository;

import com.quickmove.entity.TrackingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrackingSessionRepository extends JpaRepository<TrackingSession, Long> {

    // Active session for a ride (assumes at most one ACTIVE at a time)
    @Query("select ts from TrackingSession ts where ts.ride.id = :rideId and ts.status = 'ACTIVE'")
    Optional<TrackingSession> findActiveByRideId(@Param("rideId") Long rideId);

    // List sessions for a ride
    @Query("select ts from TrackingSession ts where ts.ride.id = :rideId")
    List<TrackingSession> findByRideId(@Param("rideId") Long rideId);

    // List sessions for a driver by driver entity id
    @Query("select ts from TrackingSession ts where ts.driver.id = :driverId")
    List<TrackingSession> findByDriverId(@Param("driverId") Long driverId);

    // List sessions for a rider by userId string
    List<TrackingSession> findByRiderUserIdOrderByStartTimeDesc(String riderUserId);
}

