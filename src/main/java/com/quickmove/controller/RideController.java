package com.quickmove.controller;

import com.quickmove.dto.RideBookingRequest;
import com.quickmove.entity.Ride;
import com.quickmove.service.RideService;
import com.quickmove.repository.RideRepository;
import com.quickmove.service.TrackingSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    @Autowired
    private RideService rideService;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private TrackingSessionService trackingSessionService;

    @PostMapping("/book")
    public ResponseEntity<Map<String, Object>> bookRide(@RequestBody RideBookingRequest req) {
        try {
            Ride ride = new Ride();
            ride.setUserId(req.getUserId());
            ride.setVehicleType(req.getVehicleType());
            ride.setFare(req.getFare()); // service will compute if null
            ride.setDropoffLocation(req.getDropoffLocation());

            Ride savedRide = rideService.createRide(ride, req.getPickupLocation());
            return ResponseEntity.ok().body(Map.of(
                    "id", savedRide.getId(),
                    "status", "success",
                    "pickup", req.getPickupLocation(),
                    "dropoff", req.getDropoffLocation(),
                    "vehicleType", req.getVehicleType(),
                    "duration", savedRide.getDuration(),
                    "distance", savedRide.getDistance()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveRide(@RequestParam(required = false) String riderUserId,
                                           @RequestParam(required = false) String driverUserId) {
        if ((riderUserId == null && driverUserId == null) || (riderUserId != null && driverUserId != null)) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Provide either riderUserId or driverUserId"));
        }
        List<String> activeStatuses = List.of("PENDING", "ACCEPTED", "IN_PROGRESS");
        Optional<Ride> rideOpt = (riderUserId != null)
                ? rideRepository.findFirstByUserIdAndStatusInOrderByIdDesc(riderUserId, activeStatuses)
                : rideRepository.findFirstByDriverIdAndStatusInOrderByIdDesc(driverUserId, activeStatuses);
        return rideOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("status", "error", "message", "No active ride")));
    }

    @GetMapping("/available-drivers")
    public ResponseEntity<List<String>> getAvailableDrivers(@RequestParam String pickupLocation) {
        return ResponseEntity.ok().body(rideService.getAvailableDrivers(pickupLocation));
    }

    @PutMapping("/update-location")
    public ResponseEntity<Map<String, Object>> updateRideLocation(@RequestBody Map<String, String> updateRequest) {
        try {
            Long id = Long.valueOf(updateRequest.get("id"));
            String pickupLocation = updateRequest.get("pickupLocation");
            String dropoffLocation = updateRequest.get("dropoffLocation");

            if (pickupLocation == null || dropoffLocation == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Missing required fields: pickupLocation, or dropoffLocation"
                ));
            }

            Ride updatedRide = rideService.updateRideLocation(id, pickupLocation, dropoffLocation);
            if (updatedRide != null) {
                return ResponseEntity.ok().body(Map.of(
                        "status", "success",
                        "message", "Locations updated",
                        "id", id,
                        "pickup", pickupLocation,
                        "dropoff", dropoffLocation,
                        "duration", updatedRide.getDuration(),
                        "distance", updatedRide.getDistance()
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "error",
                        "message", "Ride not found"
                ));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid ride ID format"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/recreate-with-location")
    public ResponseEntity<Map<String, Object>> recreateRideWithLocation(@RequestBody Map<String, String> body) {
        try {
            Long oldId = Long.valueOf(body.get("id"));
            String pickupLocation = body.get("pickupLocation");
            String dropoffLocation = body.get("dropoffLocation");
            if (pickupLocation == null || dropoffLocation == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "pickupLocation and dropoffLocation are required"));
            }
            Ride oldRide = rideRepository.findById(oldId).orElse(null);
            if (oldRide == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Old ride not found"));
            }
            // End any active tracking session for old ride and cancel it
            trackingSessionService.endActiveSessionForRide(oldId, "CANCELLED");
            rideService.cancelRide(oldId);

            // Create a new ride request with updated locations keeping rider and vehicle type
            Ride newRide = new Ride();
            newRide.setUserId(oldRide.getUserId());
            newRide.setVehicleType(oldRide.getVehicleType());
            newRide.setFare(null); // let service compute
            newRide.setDropoffLocation(dropoffLocation);
            Ride saved = rideService.createRide(newRide, pickupLocation);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "New ride created",
                    "id", saved.getId(),
                    "pickup", pickupLocation,
                    "dropoff", dropoffLocation,
                    "vehicleType", saved.getVehicleType(),
                    "duration", saved.getDuration(),
                    "distance", saved.getDistance()
            ));
        } catch (NumberFormatException ex) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid ride ID format"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", ex.getMessage()));
        }
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<Void> cancelRide(@PathVariable Long id) {
        return rideService.cancelRide(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ride> getRideById(@PathVariable Long id) {
        Ride ride = rideService.getRideById(id);
        return ride != null
                ? ResponseEntity.ok().body(ride)
                : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Ride>> getAllRides() {
        return ResponseEntity.ok().body(rideService.getAllRides());
    }

    @PostMapping("/assignDriverToRide")
    public ResponseEntity<Map<String, Object>> assignDriverToRide(@RequestParam Long driverId, @RequestParam Long rideId) {
        boolean success = rideService.assignDriverToRide(driverId, rideId);
        return success
                ? ResponseEntity.ok().body(Map.of("status", "success"))
                : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid state"));
    }

    @PostMapping("/riderAccept")
    public ResponseEntity<Map<String, Object>> riderAccept(@RequestParam Long rideId, @RequestParam String riderUserId) {
        boolean success = rideService.riderAcceptRide(rideId, riderUserId);
        return success
                ? ResponseEntity.ok(Map.of("status", "success"))
                : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid state or not allowed"));
    }

    @PostMapping("/rejectRide")
    public ResponseEntity<Map<String, Object>> rejectRide(@RequestParam Long driverId, @RequestParam Long rideId) {
        boolean success = rideService.rejectRide(driverId, rideId);
        return success
                ? ResponseEntity.ok().body(Map.of("status", "success"))
                : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid state"));
    }

    @PostMapping("/startRide")
    public ResponseEntity<Map<String, Object>> startRide(@RequestParam Long driverId, @RequestParam Long rideId) {
        boolean success = rideService.startRide(driverId, rideId);
        return success
                ? ResponseEntity.ok().body(Map.of("status", "success"))
                : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid state"));
    }

    @PostMapping("/completeRide")
    public ResponseEntity<Map<String, Object>> completeRide(@RequestParam Long driverId, @RequestParam Long rideId) {
        boolean success = rideService.completeRide(driverId, rideId);
        return success
                ? ResponseEntity.ok().body(Map.of("status", "success"))
                : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid state"));
    }

    @MessageMapping("/rideRequests")
    @SendTo("/topic/rideRequests")
    public Ride handleRideRequest(Ride ride) {
        return ride;
    }
}