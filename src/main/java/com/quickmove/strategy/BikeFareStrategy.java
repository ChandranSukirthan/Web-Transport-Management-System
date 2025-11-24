package com.quickmove.strategy;

import org.springframework.stereotype.Component;

@Component
public class BikeFareStrategy implements FareStrategy {

    @Override
    public double calculateFare(double distance, double time) {
        double baseFare = 20.0;
        double perKmRate = 10.0;
        double perMinuteRate = 0.8;

        double total = baseFare + (perKmRate * distance) + (perMinuteRate * time);
        System.out.println("[BikeFareStrategy] Total fare: " + total);
        return total;
    }
}
