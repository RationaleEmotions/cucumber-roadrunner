package com.rationaleemotions.formatter;

import com.rationaleemotions.internal.FeatureLogs;
import com.rationaleemotions.internal.ScenarioDetails;
import com.rationaleemotions.internal.ScenarioDetailsManager;
import gherkin.formatter.model.Feature;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class Utils {
    private Utils() {
    }

    static void writeDetails(String text, Map<Feature, FeatureLogs> mapping) {
        ScenarioDetails current = ScenarioDetailsManager.getCurrentScenarioDetails();
        Feature feature = current.getFeature();
        Map<ScenarioDetails, List<String>> localMapping = mapping.get(feature).getScenarioLogs();
        if (! localMapping.containsKey(current)) {
            localMapping.put(current, new LinkedList<>());
        }
        localMapping.get(current).add(text);
    }
}
