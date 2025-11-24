package com.quickmove.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${mqtt.brokerUrl}")
    private String brokerUrl;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @GetMapping("/mqtt")
    public ResponseEntity<Map<String, String>> getMqttConfig() {
        return ResponseEntity.ok(Map.of(
                "brokerUrl", brokerUrl,
                "username", username,
                "password", password
        ));
    }
}

