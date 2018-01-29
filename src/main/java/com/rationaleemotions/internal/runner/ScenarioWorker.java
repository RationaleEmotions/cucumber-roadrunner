package com.rationaleemotions.internal.runner;

import com.rationaleemotions.internal.ScenarioDetails;
import com.rationaleemotions.internal.ScenarioDetailsManager;
import cucumber.runtime.Runtime;
import cucumber.runtime.model.CucumberTagStatement;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Feature;

import java.util.concurrent.Callable;

public class ScenarioWorker implements Callable<Void> {

    private Formatter formatter;
    private Reporter reporter;
    private Runtime runtime;
    private CucumberTagStatement statement;
    private Feature feature;

    ScenarioWorker(Formatter formatter, Reporter reporter, Runtime runtime, Feature feature, CucumberTagStatement statement) {
        this.formatter = formatter;
        this.reporter = reporter;
        this.runtime = runtime;
        this.statement = statement;
        this.feature = feature;
    }

    @Override
    public Void call() {
        ScenarioDetails details = new ScenarioDetails(feature, statement);
        ScenarioDetailsManager.setScenarioDetails(details);
        statement.run(formatter, reporter, runtime);
        return null;
    }
}
