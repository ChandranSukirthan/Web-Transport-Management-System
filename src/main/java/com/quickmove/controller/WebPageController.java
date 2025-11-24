package com.quickmove.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebPageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }

    @GetMapping("/register")
    public String register() {
        return "redirect:/register.html";
    }

    @GetMapping("/become-driver")
    public String becomeDriver() {
        return "redirect:/driver-details.html";
    }

    @GetMapping("/admin/drivers")
    public String adminDrivers() {
        return "redirect:/admin-drivers.html";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    @GetMapping("/book-ride")
    public String bookRide() {
        return "redirect:/ride-booking.html";
    }
}
