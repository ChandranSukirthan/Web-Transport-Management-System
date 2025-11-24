package com.quickmove.repository;

import com.quickmove.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByUserId(String userId);
    List<Ride> findByStatus(String status);
    List<Ride> findByDriverId(String driverUserId);
    List<Ride> findByDriverIdAndStatus(String userId, String completed);

    // New methods to support fetching the latest active ride for rider/driver
    Optional<Ride> findFirstByUserIdAndStatusInOrderByIdDesc(String userId, List<String> statuses);
    Optional<Ride> findFirstByDriverIdAndStatusInOrderByIdDesc(String driverUserId, List<String> statuses);
}