package com.rationaleemotions.internal.runner;

import com.rationaleemotions.internal.RuntimeBehavior;
import cucumber.runtime.model.CucumberFeature;

public class FeatureRunnerFactory {

    public static AbstractRunner newRunner(CucumberFeature feature) {
        if (RuntimeBehavior.runScenariosInParallel()) {
            return new ParallelFeatureRunner(feature);
        }
        return new SequentialFeatureRunner(feature);
    }
}
