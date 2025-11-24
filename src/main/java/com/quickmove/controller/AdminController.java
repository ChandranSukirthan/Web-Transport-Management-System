package com.quickmove.controller;

import com.quickmove.entity.Driver;
import com.quickmove.entity.User;
import com.quickmove.repository.DriverRepository;
import com.quickmove.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/drivers/pending")
    public ResponseEntity<List<Driver>> pendingDrivers() {
        List<Driver> pending = driverRepository.findAll().stream().filter(d -> d.getApproved() == null || !d.getApproved()).toList();
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/drivers/{id}/approve")
    public ResponseEntity<?> approveDriver(@PathVariable Long id) {
        Optional<Driver> dOpt = driverRepository.findById(id);
        if (dOpt.isEmpty()) return ResponseEntity.notFound().build();
        Driver driver = dOpt.get();
        driver.setApproved(true);
        driver.setStatus("AVAILABLE");
        driverRepository.save(driver);
        // enable user account if exists
        if (driver.getUserId() != null) {
            Optional<User> uOpt = userRepository.findByEmail(driver.getUserId());
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                u.setEnabled(true);
                userRepository.save(u);
            }
        }
        return ResponseEntity.ok(Map.of("status", "approved", "driver", driver));
    }
}

