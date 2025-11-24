package com.quickmove.controller;

import com.quickmove.entity.Driver;
import com.quickmove.entity.User;
import com.quickmove.repository.DriverRepository;
import com.quickmove.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        if (name == null || email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "name,email,password are required"));
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email already registered"));
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("CUSTOMER");
        user.setEnabled(true);
        User saved = userRepository.save(user);
        // avoid returning password
        saved.setPassword(null);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || password == null) return ResponseEntity.badRequest().body(Map.of("error", "email and password required"));
        Optional<User> u = userRepository.findByEmail(email);
        if (u.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        User user = u.get();
        if (!user.isEnabled()) return ResponseEntity.status(403).body(Map.of("error", "account not enabled"));
        if (!passwordEncoder.matches(password, user.getPassword())) return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        // Mask password before returning
        user.setPassword(null);
        return ResponseEntity.ok(Map.of("message", "login successful", "user", user));
    }

    @PostMapping("/driver/register")
    public ResponseEntity<?> registerDriver(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String vehicleType = body.get("vehicleType");
        String numberPlate = body.get("numberPlate");
        if (name == null || email == null || password == null || vehicleType == null || numberPlate == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "name,email,password,vehicleType,numberPlate are required"));
        }

        // If user already exists, don't fail — we will convert/update the user and create/update a Driver record
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User savedUser;
        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            // Update role to DRIVER (promote customer to driver or keep driver)
            existing.setRole("DRIVER");
            // Update password if provided (we always have a password from the onboarding flow)
            existing.setPassword(passwordEncoder.encode(password));
            // Keep existing enabled flag (do not automatically disable a previously enabled customer)
            savedUser = userRepository.save(existing);
        } else {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole("DRIVER");
            user.setEnabled(false); // enabled after admin approval for new driver accounts
            savedUser = userRepository.save(user);
        }

        // Create or update Driver record linked by user email
        Optional<Driver> existingDriverOpt = driverRepository.findByUserId(email);
        Driver driverEntity;
        if (existingDriverOpt.isPresent()) {
            driverEntity = existingDriverOpt.get();
            driverEntity.setName(name);
            driverEntity.setVehicleType(vehicleType);
            driverEntity.setNumberPlate(numberPlate);
            driverEntity.setApproved(false); // mark pending approval when details change
            driverEntity.setStatus("OFFLINE");
            driverEntity = driverRepository.save(driverEntity);
        } else {
            Driver driver = new Driver();
            driver.setName(name);
            driver.setUserId(savedUser.getEmail());
            driver.setVehicleType(vehicleType);
            driver.setNumberPlate(numberPlate);
            driver.setStatus("OFFLINE");
            driver.setApproved(false);
            driverEntity = driverRepository.save(driver);
        }

        // avoid returning password
        savedUser.setPassword(null);
        return ResponseEntity.ok(Map.of("user", savedUser, "driver", driverEntity));
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> listUsers() {
        List<User> users = userRepository.findAll();
        // mask passwords
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }
}
