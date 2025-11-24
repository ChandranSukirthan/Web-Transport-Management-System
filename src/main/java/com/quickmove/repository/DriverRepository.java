package com.quickmove.repository;

import com.quickmove.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    // Custom query to return Optional<Driver> for a unique userId
    @Query("SELECT d FROM Driver d WHERE d.userId = ?1")
    Optional<Driver> findByUserId(String userId);

    // Returns list of drivers by status (can be multiple)
    List<Driver> findByStatus(String status);
}