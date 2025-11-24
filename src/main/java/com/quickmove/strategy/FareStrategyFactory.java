package com.quickmove.strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FareStrategyFactory {

    private final Map<String, FareStrategy> strategies = new HashMap<>();

    @Autowired
    public FareStrategyFactory(List<FareStrategy> strategyList) {
        System.out.println("Initializing FareStrategyFactory...");
        for (FareStrategy strategy : strategyList) {
            String key = strategy.getClass().getSimpleName()
                    .replace("FareStrategy", "")
                    .toLowerCase();
            strategies.put(key, strategy);
            System.out.println("Registered FareStrategy: " + strategy.getClass().getSimpleName() + " → key = " + key);
        }
        System.out.println("Total strategies registered: " + strategies.size());
    }

    public FareStrategy getStrategy(String vehicleType) {
        if (vehicleType == null) {
            System.out.println("Vehicle type is null. Defaulting to 'car'.");
            return strategies.getOrDefault("car", null);
        }

        FareStrategy selected = strategies.getOrDefault(vehicleType.toLowerCase(), strategies.get("car"));
        System.out.println("Selected strategy for '" + vehicleType + "': " +
                (selected != null ? selected.getClass().getSimpleName() : "null"));
        return selected;
    }
}
