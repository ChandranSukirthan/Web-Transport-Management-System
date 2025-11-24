package com.quickmove.controller;

import com.quickmove.entity.Driver;
import com.quickmove.entity.Ride;
import com.quickmove.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @PostMapping
    public ResponseEntity<Driver> createDriver(@RequestBody Driver driver) {
        try {
            return ResponseEntity.ok().body(driverService.createDriver(driver));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Driver> getDriver(@PathVariable Long id) {
        Driver driver = driverService.getDriverById(id);
        return driver != null ? ResponseEntity.ok().body(driver) : ResponseEntity.notFound().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Driver> getDriverByUserId(@PathVariable String userId) {
        Driver driver = driverService.getDriverByUserId(userId);
        return driver != null ? ResponseEntity.ok().body(driver) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Driver>> getAllDrivers() {
        return ResponseEntity.ok().body(driverService.getAllDrivers());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Driver>> getAvailableDrivers() {
        return ResponseEntity.ok().body(driverService.getAvailableDrivers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Driver> updateDriver(@PathVariable Long id, @RequestBody Driver driver) {
        Driver updated = driverService.updateDriver(id, driver);
        return updated != null ? ResponseEntity.ok().body(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDriver(@PathVariable Long id) {
        return driverService.deleteDriver(id) ? ResponseEntity.ok().body("Deleted") : ResponseEntity.notFound().build();
    }

    @PostMapping("/{driverId}/rides/{rideId}/accept")
    public ResponseEntity<Map<String, Object>> acceptRide(@PathVariable Long driverId, @PathVariable Long rideId) {
        boolean success = driverService.assignDriverToRide(driverId, rideId);
        return success ? ResponseEntity.ok().body(Map.of("status", "success", "message", "Ride accepted")) : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to accept ride"));
    }

    @PostMapping("/{driverId}/rides/{rideId}/reject")
    public ResponseEntity<Map<String, Object>> rejectRide(@PathVariable Long driverId, @PathVariable Long rideId) {
        boolean success = driverService.rejectRide(driverId, rideId);
        return success ? ResponseEntity.ok().body(Map.of("status", "success", "message", "Ride rejected")) : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to reject ride"));
    }

    @PostMapping("/{driverId}/rides/{rideId}/start")
    public ResponseEntity<Map<String, Object>> startRide(@PathVariable Long driverId, @PathVariable Long rideId) {
        boolean success = driverService.startRide(driverId, rideId);
        return success ? ResponseEntity.ok().body(Map.of("status", "success", "message", "Ride started")) : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to start ride"));
    }

    @PostMapping("/{driverId}/rides/{rideId}/complete")
    public ResponseEntity<Map<String, Object>> completeRide(@PathVariable Long driverId, @PathVariable Long rideId) {
        boolean success = driverService.completeRide(driverId, rideId);
        return success ? ResponseEntity.ok().body(Map.of("status", "success", "message", "Ride completed")) : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to complete ride"));
    }

    @GetMapping("/{driverId}/rides")
    public ResponseEntity<List<Ride>> getDriverRideHistory(@PathVariable Long driverId) {
        Driver driver = driverService.getDriverById(driverId);
        return driver != null ? ResponseEntity.ok().body(driverService.getDriverRideHistory(driver.getUserId())) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{driverId}/earnings")
    public ResponseEntity<Double> getDriverEarnings(@PathVariable Long driverId) {
        Driver driver = driverService.getDriverById(driverId);
        return driver != null ? ResponseEntity.ok().body(driverService.getDriverEarnings(driver.getUserId())) : ResponseEntity.notFound().build();
    }
}