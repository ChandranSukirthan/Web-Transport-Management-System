package com.quickmove.strategy;

import org.springframework.stereotype.Component;

@Component
public class AutoFareStrategy implements FareStrategy {

    @Override
    public double calculateFare(double distance, double time) {
        double baseFare = 40.0;
        double perKmRate = 15.0;
        double perMinuteRate = 1.2;

        double total = baseFare + (perKmRate * distance) + (perMinuteRate * time);
        System.out.println("🚖 [AutoFareStrategy] Total fare: " + total);
        return total;
    }
}
