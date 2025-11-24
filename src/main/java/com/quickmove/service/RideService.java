package com.quickmove.service;

import com.quickmove.entity.Driver;
import com.quickmove.entity.Ride;
import com.quickmove.repository.RideRepository;
import com.quickmove.strategy.FareStrategy;
import com.quickmove.strategy.FareStrategyFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RideService {

    private static final Logger logger = LoggerFactory.getLogger(RideService.class);

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DriverService driverService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FareStrategyFactory fareStrategyFactory;

    private static final String GEOAPIFY_API_KEY = "9efd1d5a33794a978148be1526c83da6";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Ride createRide(Ride ride, String currentLocation) {
        if (ride == null || currentLocation == null || ride.getVehicleType() == null || ride.getUserId() == null) {
            throw new IllegalArgumentException("All ride details (ride, currentLocation, vehicleType, userId) are required");
        }

        ride.setPickupLocation(currentLocation);
        ride.setStatus("PENDING");
        ride.setDriverId(null);

        if (ride.getDropoffLocation() != null) {
            Map<String, Double> routeInfo = calculateRoute(currentLocation, ride.getDropoffLocation());
            ride.setDistance(routeInfo.getOrDefault("distance", 0.0));
            ride.setDuration(routeInfo.getOrDefault("duration", 0.0));

            FareStrategy strategy = fareStrategyFactory.getStrategy(ride.getVehicleType());
            if (strategy != null) {
                ride.setFare(strategy.calculateFare(ride.getDistance(), ride.getDuration()));
            } else {
                ride.setFare(0.0);
            }
        } else {
            ride.setDistance(null);
            ride.setDuration(null);
            ride.setFare(0.0);
        }

        Ride savedRide = rideRepository.save(ride);
        messagingTemplate.convertAndSend("/topic/rideRequests", savedRide);
        logger.info("Created ride ID: {} with status: {}", savedRide.getId(), savedRide.getStatus());
        return savedRide;
    }

    public List<String> getAvailableDrivers(String pickupLocation) {
        List<Driver> availableDrivers = driverService.getAvailableDrivers();
        return availableDrivers.stream().map(Driver::getUserId).toList();
    }

    public Ride updateRideLocation(Long id, String pickupLocation, String dropoffLocation) {
        Optional<Ride> rideOpt = rideRepository.findById(id);
        if (rideOpt.isPresent() && pickupLocation != null && dropoffLocation != null) {
            Ride ride = rideOpt.get();
            ride.setPickupLocation(pickupLocation);
            ride.setDropoffLocation(dropoffLocation);

            Map<String, Double> routeInfo = calculateRoute(pickupLocation, dropoffLocation);
            ride.setDistance(routeInfo.getOrDefault("distance", 0.0));
            ride.setDuration(routeInfo.getOrDefault("duration", 0.0));

            FareStrategy strategy = fareStrategyFactory.getStrategy(ride.getVehicleType());
            if (strategy != null) {
                ride.setFare(strategy.calculateFare(ride.getDistance(), ride.getDuration()));
            }

            ride.setStatus("PENDING");
            Ride updatedRide = rideRepository.save(ride);
            messagingTemplate.convertAndSend("/topic/rideRequests", updatedRide);
            return updatedRide;
        }
        return null;
    }

    public boolean cancelRide(Long id) {
        Optional<Ride> rideOpt = rideRepository.findById(id);
        if (rideOpt.isPresent()) {
            Ride ride = rideOpt.get();
            if (!"COMPLETED".equals(ride.getStatus()) && !"CANCELLED".equals(ride.getStatus())) {
                ride.setStatus("CANCELLED");
                rideRepository.save(ride);
                messagingTemplate.convertAndSend("/topic/rideRequests", ride);
                return true;
            }
        }
        return false;
    }

    public Ride getRideById(Long id) {
        return rideRepository.findById(id).orElse(null);
    }

    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }

    public boolean assignDriverToRide(Long driverId, Long rideId) {
        Driver driver = driverService.getDriverById(driverId);
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (driver != null && rideOpt.isPresent()) {
            Ride ride = rideOpt.get();
            if ("AVAILABLE".equals(driver.getStatus()) && "PENDING".equals(ride.getStatus()) && ride.getDriverId() == null) {
                driver.setStatus("BUSY");
                driverService.updateDriver(driverId, driver);
                ride.setDriverId(driver.getUserId());
                ride.setStatus("ACCEPTED");
                rideRepository.save(ride);
                messagingTemplate.convertAndSend("/topic/rideUpdates/" + rideId, ride);
                return true;
            }
        }
        return false;
    }

    public boolean riderAcceptRide(Long rideId, String riderUserId) {
        if (rideId == null || riderUserId == null) return false;
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (rideOpt.isEmpty()) return false;

        Ride ride = rideOpt.get();
        // Rider can accept if they are the owner of the ride and the ride is either PENDING (no driver yet) or ACCEPTED (driver assigned)
        if (!riderUserId.equals(ride.getUserId())) {
            logger.warn("riderAcceptRide: user {} is not owner of ride {}", riderUserId, rideId);
            return false;
        }

        String currentStatus = ride.getStatus();
        if ("ACCEPTED".equals(currentStatus) || "PENDING".equals(currentStatus)) {
            // Mark as accepted by rider; keep canonical status as ACCEPTED
            ride.setStatus("ACCEPTED");
            rideRepository.save(ride);
            try {
                messagingTemplate.convertAndSend("/topic/rideUpdates/" + rideId, ride);
            } catch (Exception e) {
                logger.warn("Failed to notify websocket for riderAcceptRide: {}", e.getMessage());
            }
            return true;
        }

        logger.warn("riderAcceptRide: ride {} not in a state that rider can accept (current={})", rideId, currentStatus);
        return false;
    }

    public boolean rejectRide(Long driverId, Long rideId) {
        Driver driver = driverService.getDriverById(driverId);
        Optional<Ride> rideOpt = rideRepository.findById(rideId);

        if (driver != null && rideOpt.isPresent()) {
            Ride ride = rideOpt.get();

            if ("ACCEPTED".equals(ride.getStatus()) &&
                    ride.getDriverId() != null &&
                    ride.getDriverId().equals(driver.getUserId())) {

                ride.setDriverId(null);
                ride.setStatus("PENDING");
                driver.setStatus("AVAILABLE");

                driverService.updateDriver(driverId, driver);
                rideRepository.save(ride);

                messagingTemplate.convertAndSend("/topic/rideRequests", ride);
                messagingTemplate.convertAndSend("/topic/rideUpdates/" + rideId, ride);

                logger.info("Driver {} rejected ride {}. Ride set to PENDING again.", driverId, rideId);
                return true;
            }
        }

        logger.warn("Failed to reject ride {} by driver {}: Invalid state", rideId, driverId);
        return false;
    }

    public boolean startRide(Long driverId, Long rideId) {
        Driver driver = driverService.getDriverById(driverId);
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (driver != null && rideOpt.isPresent()) {
            Ride ride = rideOpt.get();
            if ("BUSY".equals(driver.getStatus()) &&
                    "ACCEPTED".equals(ride.getStatus()) &&
                    ride.getDriverId().equals(driver.getUserId())) {
                ride.setStatus("IN_PROGRESS");
                rideRepository.save(ride);
                messagingTemplate.convertAndSend("/topic/rideUpdates/" + rideId, ride);
                return true;
            }
        }
        return false;
    }

    public boolean completeRide(Long driverId, Long rideId) {
        Driver driver = driverService.getDriverById(driverId);
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (driver != null && rideOpt.isPresent()) {
            Ride ride = rideOpt.get();
            if ("BUSY".equals(driver.getStatus()) &&
                    "IN_PROGRESS".equals(ride.getStatus()) &&
                    ride.getDriverId().equals(driver.getUserId())) {
                driver.setStatus("AVAILABLE");
                driverService.updateDriver(driverId, driver);
                ride.setStatus("COMPLETED");
                rideRepository.save(ride);
                messagingTemplate.convertAndSend("/topic/rideUpdates/" + rideId, ride);
                return true;
            }
        }
        return false;
    }

    private Map<String, Double> calculateRoute(String pickup, String dropoff) {
        Map<String, Double> result = new HashMap<>();
        try {
            if (pickup == null || dropoff == null) {
                throw new IllegalArgumentException("Pickup and dropoff locations are required");
            }

            String pickupCoords = geocodeAddress(pickup);
            String dropoffCoords = geocodeAddress(dropoff);

            if (pickupCoords == null || dropoffCoords == null) {
                logger.warn("Geocoding failed. Using default Colombo coordinates.");
                pickupCoords = "6.9271,79.8612";
                dropoffCoords = "6.9271,79.8612";
            }

            String[] p = pickupCoords.split(",");
            String[] d = dropoffCoords.split(",");
            String url = "https://api.geoapify.com/v1/routing?waypoints=" + p[0] + "," + p[1] + "|" + d[0] + "," + d[1] +
                    "&mode=drive&apiKey=" + GEOAPIFY_API_KEY;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            JsonNode properties = root.path("features").get(0).path("properties");
            result.put("distance", properties.path("distance").asDouble() / 1000.0);
            result.put("duration", properties.path("time").asDouble() / 60.0);
        } catch (Exception e) {
            logger.error("Route calculation failed: {}", e.getMessage());
            result.put("distance", 5.0);
            result.put("duration", 10.0);
        }
        return result;
    }

    private String geocodeAddress(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://api.geoapify.com/v1/geocode/search?text=" + encoded + "&apiKey=" + GEOAPIFY_API_KEY;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (root.path("features").size() > 0) {
                JsonNode feature = root.path("features").get(0);
                double lat = feature.path("geometry").path("coordinates").get(1).asDouble();
                double lon = feature.path("geometry").path("coordinates").get(0).asDouble();
                return lat + "," + lon;
            }
        } catch (Exception e) {
            logger.error("Geocoding error: {}", e.getMessage());
        }
        return null;
    }
}
