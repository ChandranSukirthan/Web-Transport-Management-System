package com.quickmove.controller;

import com.quickmove.entity.Driver;
import com.quickmove.entity.Ride;
import com.quickmove.entity.TrackingSession;
import com.quickmove.repository.DriverRepository;
import com.quickmove.repository.RideRepository;
import com.quickmove.service.TrackingSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tracking-sessions")
public class TrackingSessionController {

    @Autowired
    private TrackingSessionService trackingSessionService;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private DriverRepository driverRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            Long rideId = Long.valueOf(body.get("rideId").toString());
            Long driverId = Long.valueOf(body.get("driverId").toString());
            String riderUserId = body.get("riderUserId").toString();
            String createdBy = body.getOrDefault("createdBy", "system").toString();
            Ride ride = rideRepository.findById(rideId).orElse(null);
            Driver driver = driverRepository.findById(driverId).orElse(null);
            if (ride == null || driver == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid ride or driver"));
            }
            TrackingSession session = trackingSessionService.ensureActiveSession(ride, driver, riderUserId, createdBy);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/ride/{rideId}/end")
    public ResponseEntity<?> endActiveForRide(@PathVariable Long rideId, @RequestParam Long driverId) {
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        Optional<Driver> driverOpt = driverRepository.findById(driverId);
        if (rideOpt.isEmpty() || driverOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid ride or driver"));
        }
        Ride ride = rideOpt.get();
        Driver driver = driverOpt.get();
        if (ride.getDriverId() == null || !ride.getDriverId().equals(driver.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("status", "error", "message", "Only the assigned driver can end the trip"));
        }
        if (trackingSessionService.getActiveByRideId(rideId).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("status", "error", "message", "No active session for this ride"));
        }
        trackingSessionService.endActiveSessionForRide(rideId, "ENDED");
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return trackingSessionService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<TrackingSession>> list(@RequestParam(required = false) Long rideId,
                                                      @RequestParam(required = false) Long driverId,
                                                      @RequestParam(required = false) String riderUserId) {
        if (rideId != null) return ResponseEntity.ok(trackingSessionService.listByRideId(rideId));
        if (driverId != null) return ResponseEntity.ok(trackingSessionService.listByDriverId(driverId));
        if (riderUserId != null) return ResponseEntity.ok(trackingSessionService.listByRiderUserId(riderUserId));
        return ResponseEntity.ok(trackingSessionService.listAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String status = body.get("status") != null ? body.get("status").toString() : null;
            LocalDateTime endTime = null;
            if (body.get("endTime") != null) endTime = LocalDateTime.parse(body.get("endTime").toString());
            TrackingSession updated = trackingSessionService.updateSession(id, status, endTime);
            if (updated == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return trackingSessionService.deleteSession(id)
                ? ResponseEntity.ok(Map.of("status", "deleted"))
                : ResponseEntity.notFound().build();
    }

    // New: active session summary for a ride (rider/driver acceptance + session status)
    @GetMapping("/ride/{rideId}/active-summary")
    public ResponseEntity<Map<String, Object>> activeSummary(@PathVariable Long rideId) {
        return ResponseEntity.ok(trackingSessionService.getActiveSummary(rideId));
    }

    // New: rider acceptance for active session of a ride
    @PostMapping("/ride/{rideId}/rider-accept")
    public ResponseEntity<Map<String, Object>> riderAccept(@PathVariable Long rideId,
                                                           @RequestParam(required = false) String riderUserId) {
        boolean ok = trackingSessionService.riderAcceptForRide(rideId, riderUserId);
        return ok ? ResponseEntity.ok(Map.of("status", "success"))
                  : ResponseEntity.badRequest().body(Map.of("status", "error", "message", "No active session or unauthorized"));
    }
}
