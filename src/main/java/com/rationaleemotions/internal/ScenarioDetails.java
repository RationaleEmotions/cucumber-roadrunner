package com.rationaleemotions.internal;

import cucumber.runtime.model.CucumberTagStatement;
import gherkin.formatter.model.Feature;

public class ScenarioDetails {
    private Feature feature;
    private CucumberTagStatement scenario;

    public ScenarioDetails(Feature feature, CucumberTagStatement scenario) {
        this.feature = feature;
        this.scenario = scenario;
    }

    public Feature getFeature() {
        return feature;
    }

    public CucumberTagStatement getScenario() {
        return scenario;
    }
}
