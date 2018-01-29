package com.rationaleemotions.internal.runner;

import com.rationaleemotions.internal.ScenarioDetails;
import com.rationaleemotions.internal.ScenarioDetailsManager;
import cucumber.runtime.Runtime;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;

public abstract class AbstractRunner {
    private CucumberFeature wrappedFeature;

    AbstractRunner(CucumberFeature wrappedFeature) {
        this.wrappedFeature = wrappedFeature;
    }

    public final CucumberFeature getFeature() {
        return wrappedFeature;
    }

    public void run(Formatter formatter, Reporter reporter, Runtime runtime) {
        ScenarioDetailsManager.setScenarioDetails(new ScenarioDetails(wrappedFeature.getGherkinFeature(), null));
        formatter.uri(wrappedFeature.getPath());
        formatter.feature(wrappedFeature.getGherkinFeature());
        runScenario(formatter, reporter, runtime);
        formatter.eof();
    }

    abstract void runScenario(Formatter formatter, Reporter reporter, Runtime runtime);
}
