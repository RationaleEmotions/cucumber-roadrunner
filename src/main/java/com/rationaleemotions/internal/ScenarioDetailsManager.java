package com.rationaleemotions.internal;

public class ScenarioDetailsManager {

    private static ThreadLocal<ScenarioDetails> allDetails = new InheritableThreadLocal<>();

    public static ScenarioDetails getCurrentScenarioDetails() {
        return allDetails.get();
    }

    public static void setScenarioDetails(ScenarioDetails details) {
        allDetails.set(details);
    }

}
