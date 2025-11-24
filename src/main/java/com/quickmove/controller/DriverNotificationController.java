package com.quickmove.controller;

import com.quickmove.entity.Ride;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class DriverNotificationController {


    @MessageMapping("/rideRequest")
    @SendTo("/topic/rideRequests")
    public Ride sendRideRequest(Ride ride) {
        return ride; // Broadcast new ride request to all available drivers
    }
}