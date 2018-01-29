package com.rationaleemotions.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FeatureLogs {
    private StringBuffer appender = new StringBuffer();
    private Map<ScenarioDetails, List<String>> scenarioLogs = new ConcurrentHashMap<>();

    public StringBuffer getAppender() {
        return appender;
    }

    public Map<ScenarioDetails, List<String>> getScenarioLogs() {
        return scenarioLogs;
    }
}
