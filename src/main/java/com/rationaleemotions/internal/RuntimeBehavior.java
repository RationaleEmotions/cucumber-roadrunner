package com.rationaleemotions.internal;

public final class RuntimeBehavior {
    private RuntimeBehavior() {
    }

    public static boolean runScenariosInParallel() {
        return Boolean.parseBoolean(System.getProperty("roadrunner.parallel", "true"));
    }

    public static int getThreadCount() {
        return Integer.parseInt(System.getProperty("roadrunner.threads", "10"));
    }
}
