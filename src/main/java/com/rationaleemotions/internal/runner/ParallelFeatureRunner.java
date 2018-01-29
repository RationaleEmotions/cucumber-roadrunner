package com.rationaleemotions.internal.runner;

import com.rationaleemotions.internal.RuntimeBehavior;
import cucumber.runtime.Runtime;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberTagStatement;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 */
class ParallelFeatureRunner extends AbstractRunner {

    ParallelFeatureRunner(CucumberFeature wrappedFeature) {
        super(wrappedFeature);
    }

    @Override
    void runScenario(Formatter formatter, Reporter reporter, Runtime runtime) {
        List<Callable<Void>> workers = new ArrayList<>();
        for (CucumberTagStatement scenario : getFeature().getFeatureElements()) {
            Feature feature = getFeature().getGherkinFeature();
            Callable<Void> worker = new ScenarioWorker(formatter, reporter, runtime, feature, scenario);
            workers.add(worker);
        }
        ExecutorService service = Executors.newFixedThreadPool(RuntimeBehavior.getThreadCount());
        try {
            List<Future<Void>> results = service.invokeAll(workers);
            for (Future<Void> result : results) {
                result.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
