package com.quickmove.strategy;

import org.springframework.stereotype.Component;

@Component
public class CarFareStrategy implements FareStrategy {

    @Override
    public double calculateFare(double distance, double time) {
        double baseFare = 70.0;
        double perKmRate = 25.0;
        double perMinuteRate = 2.0;

        double total = baseFare + (perKmRate * distance) + (perMinuteRate * time);
        System.out.println("[CarFareStrategy] Total fare: " + total);
        return total;
    }
}