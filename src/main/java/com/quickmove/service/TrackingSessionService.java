package com.quickmove.service;

import com.quickmove.entity.Driver;
import com.quickmove.entity.Ride;
import com.quickmove.entity.TrackingSession;
import com.quickmove.repository.TrackingSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TrackingSessionService {

    @Autowired
    private TrackingSessionRepository trackingSessionRepository;

    public TrackingSession createSession(Ride ride, Driver driver, String riderUserId, String createdBy) {
        TrackingSession session = new TrackingSession();
        session.setRide(ride);
        session.setDriver(driver);
        session.setRiderUserId(riderUserId);
        session.setCreatedBy(createdBy);
        session.setStatus("ACTIVE");
        session.setStartTime(LocalDateTime.now());
        // set acceptance by creator (driver only)
        if ("driver".equalsIgnoreCase(createdBy)) {
            session.setDriverAccepted(true);
        }
        return trackingSessionRepository.save(session);
    }

    public TrackingSession ensureActiveSession(Ride ride, Driver driver, String riderUserId, String createdBy) {
        Optional<TrackingSession> existingOpt = trackingSessionRepository.findActiveByRideId(ride.getId());
        if (existingOpt.isPresent()) {
            TrackingSession existing = existingOpt.get();
            boolean changed = false;
            if ("driver".equalsIgnoreCase(createdBy)) {
                if (existing.getDriverAccepted() == null || !existing.getDriverAccepted()) { existing.setDriverAccepted(true); changed = true; }
            }
            if (changed) return trackingSessionRepository.save(existing);
            return existing;
        }
        return createSession(ride, driver, riderUserId, createdBy);
    }

    public Optional<TrackingSession> getById(Long id) {
        return trackingSessionRepository.findById(id);
    }

    public List<TrackingSession> listAll() {
        return trackingSessionRepository.findAll();
    }

    public List<TrackingSession> listByRideId(Long rideId) {
        return trackingSessionRepository.findByRideId(rideId);
    }

    public List<TrackingSession> listByDriverId(Long driverId) {
        return trackingSessionRepository.findByDriverId(driverId);
    }

    public List<TrackingSession> listByRiderUserId(String riderUserId) {
        return trackingSessionRepository.findByRiderUserIdOrderByStartTimeDesc(riderUserId);
    }

    public Optional<TrackingSession> getActiveByRideId(Long rideId) {
        return trackingSessionRepository.findActiveByRideId(rideId);
    }

    public TrackingSession updateSession(Long id, String status, LocalDateTime endTime) {
        TrackingSession session = trackingSessionRepository.findById(id).orElse(null);
        if (session == null) return null;
        if (status != null) session.setStatus(status);
        if (endTime != null) session.setEndTime(endTime);
        return trackingSessionRepository.save(session);
    }

    public boolean deleteSession(Long id) {
        if (trackingSessionRepository.existsById(id)) {
            trackingSessionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public void endActiveSessionForRide(Long rideId, String finalStatus) {
        trackingSessionRepository.findActiveByRideId(rideId).ifPresent(ts -> {
            ts.setStatus(finalStatus != null ? finalStatus : "ENDED");
            ts.setEndTime(LocalDateTime.now());
            trackingSessionRepository.save(ts);
        });
    }

    // Mark rider acceptance for the active session of a ride
    public boolean riderAcceptForRide(Long rideId, String riderUserId) {
        Optional<TrackingSession> opt = trackingSessionRepository.findActiveByRideId(rideId);
        if (opt.isEmpty()) return false;
        TrackingSession ts = opt.get();
        // Validate rider
        String expectedRider = ts.getRiderUserId() != null ? ts.getRiderUserId() : (ts.getRide() != null ? ts.getRide().getUserId() : null);
        if (expectedRider != null && riderUserId != null && !expectedRider.equals(riderUserId)) {
            return false;
        }
        ts.setRiderAccepted(true);
        trackingSessionRepository.save(ts);
        return true;
    }

    // Provide a lightweight summary for polling from UI
    public Map<String, Object> getActiveSummary(Long rideId) {
        Map<String, Object> map = new HashMap<>();
        Optional<TrackingSession> opt = trackingSessionRepository.findActiveByRideId(rideId);
        if (opt.isEmpty()) {
            map.put("hasActive", false);
            return map;
        }
        TrackingSession ts = opt.get();
        map.put("hasActive", true);
        map.put("status", ts.getStatus());
        map.put("riderAccepted", Boolean.TRUE.equals(ts.getRiderAccepted()));
        map.put("driverAccepted", Boolean.TRUE.equals(ts.getDriverAccepted()));
        return map;
    }
}
