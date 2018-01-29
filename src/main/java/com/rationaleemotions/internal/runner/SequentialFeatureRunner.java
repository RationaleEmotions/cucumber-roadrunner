package com.rationaleemotions.internal.runner;

import com.rationaleemotions.internal.ScenarioDetails;
import com.rationaleemotions.internal.ScenarioDetailsManager;
import cucumber.runtime.Runtime;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberTagStatement;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;

/**
 *
 */
class SequentialFeatureRunner extends AbstractRunner {

    SequentialFeatureRunner(CucumberFeature wrappedFeature) {
        super(wrappedFeature);
    }

    @Override
    void runScenario(Formatter formatter, Reporter reporter, Runtime runtime) {
        for (CucumberTagStatement scenario : getFeature().getFeatureElements()) {
            ScenarioDetails details = new ScenarioDetails(getFeature().getGherkinFeature(), scenario);
            ScenarioDetailsManager.setScenarioDetails(details);
            scenario.run(formatter, reporter, runtime);
        }
    }
}