package com.quickmove.service;

import com.quickmove.entity.Driver;
import com.quickmove.entity.Ride;
import com.quickmove.repository.DriverRepository;
import com.quickmove.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class handling driver-related operations, including assignment, rejection,
 * and ride management with WebSocket notifications.
 */
@Service
public class DriverService {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Driver createDriver(Driver driver) {
        if (driver == null || driver.getName() == null || driver.getUserId() == null ||
                driver.getVehicleType() == null || driver.getNumberPlate() == null) {
            throw new IllegalArgumentException("All driver details (name, userId, vehicleType, numberPlate, phone) are required");
        }
        driver.setStatus("AVAILABLE");
        return driverRepository.save(driver);
    }

    public boolean assignDriverToRide(Long driverId, Long rideId) {
        Optional<Driver> driverOpt = driverRepository.findById(driverId);
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        System.out.println("Attempting to assign ride " + driverOpt + " to driver " + rideOpt);
        if (driverOpt.isPresent() && rideOpt.isPresent()) {
            Driver driver = driverOpt.get();
            Ride ride = rideOpt.get();
            System.out.println("Driver status: " + driver.getStatus() + ", Ride status: " + ride.getStatus() + ", Ride driverId: " + ride.getDriverId());
            if ("AVAILABLE".equals(driver.getStatus()) && "PENDING".equals(ride.getStatus()) && ride.getDriverId() == null) {
                driver.setStatus("BUSY");
                ride.setDriverId(driver.getUserId());
                ride.setStatus("ACCEPTED");
                driverRepository.save(driver);
                Ride savedRide = rideRepository.save(ride);
                messagingTemplate.convertAndSend("/topic/rideUpdates/" + rideId, savedRide);
                return true;
            }
        }
        System.out.println("Failed to assign ride " + rideId + " to driver " + driverId + ": Invalid state");
        return false;
    }

    public boolean rejectRide(Long driverId, Long rideId) {
        Optional<Driver> driverOpt = driverRepository.findById(driverId);
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (driverOpt.isPresent() && rideOpt.isPresent()) {
            Driver driver = driverOpt.get();
            Ride ride = rideOpt.get();
            if ("PENDING".equals(ride.getStatus())) {
                return true;
            } else if ("ACCEPTED".equals(ride.getStatus()) && ride.getDriverId() != null && ride.getDriverId().equals(driver.getUserId())) {
                ride.setDriverId(null);
                ride.setStatus("PENDING");
                driver.setStatus("AVAILABLE");
                driverRepository.save(driver);
                Ride savedRide = rideRepository.save(ride);
                messagingTemplate.convertAndSend("/topic/rideRequests", savedRide);
                messagingTemplate.convertAndSend("/topic/rideUpdates/" + rideId, savedRide);
                return true;
            }
        }
        System.out.println("Failed to reject ride " + rideId + " by driver " + driverId + ": Invalid state or not found");
        return false;
    }

    public Driver getDriverById(Long id) {
        return driverRepository.findById(id).orElse(null);
    }

    public Driver getDriverByUserId(String userId) {
        return driverRepository.findByUserId(userId).orElse(null);
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public List<Driver> getAvailableDrivers() {
        return driverRepository.findByStatus("AVAILABLE");
    }

    public Driver updateDriver(Long id, Driver driver) {
        Optional<Driver> driverOpt = driverRepository.findById(id);
        if (driverOpt.isPresent()) {
            Driver existing = driverOpt.get();
            existing.setName(driver.getName());
            existing.setUserId(driver.getUserId());
            existing.setVehicleType(driver.getVehicleType());
            existing.setNumberPlate(driver.getNumberPlate());
            existing.setStatus(driver.getStatus());
            return driverRepository.save(existing);
        }
        return null;
    }

    public boolean deleteDriver(Long id) {
        if (driverRepository.existsById(id)) {
            driverRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean startRide(Long driverId, Long rideId) {
        Optional<Driver> driverOpt = driverRepository.findById(driverId);
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (driverOpt.isPresent() && rideOpt.isPresent()) {
            Driver driver = driverOpt.get();
            Ride ride = rideOpt.get();
            if ("BUSY".equals(driver.getStatus()) && "ACCEPTED".equals(ride.getStatus()) && ride.getDriverId().equals(driver.getUserId())) {
                ride.setStatus("IN_PROGRESS");
                Ride savedRide = rideRepository.save(ride);
                messagingTemplate.convertAndSend("/topic/rideUpdates/" + rideId, savedRide);
                return true;
            }
        }
        System.out.println("Failed to start ride " + rideId + " for driver " + driverId + ": Invalid state");
        return false;
    }

    public boolean completeRide(Long driverId, Long rideId) {
        Optional<Driver> driverOpt = driverRepository.findById(driverId);
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (driverOpt.isPresent() && rideOpt.isPresent()) {
            Driver driver = driverOpt.get();
            Ride ride = rideOpt.get();
            System.out.println("Driver status: " + driver.getStatus() + ", Ride status: " + ride.getStatus() + ", Ride driverId: " + ride.getDriverId());
            if ("BUSY".equals(driver.getStatus()) && "IN_PROGRESS".equals(ride.getStatus()) && ride.getDriverId().equals(driver.getUserId())) {
                driver.setStatus("AVAILABLE");
                ride.setStatus("COMPLETED");
                driverRepository.save(driver);
                Ride savedRide = rideRepository.save(ride);
                messagingTemplate.convertAndSend("/topic/rideUpdates/" + rideId, savedRide);
                return true;
            }
        }
        System.out.println("Failed to complete ride " + rideId + " for driver " + driverId + ": Invalid state");
        return false;
    }

    public List<Ride> getDriverRideHistory(String userId) {
        return rideRepository.findByDriverId(userId);
    }

    public Double getDriverEarnings(String userId) {
        List<Ride> rides = rideRepository.findByDriverIdAndStatus(userId, "COMPLETED");
        return rides.stream().mapToDouble(Ride::getFare).sum();
    }
}