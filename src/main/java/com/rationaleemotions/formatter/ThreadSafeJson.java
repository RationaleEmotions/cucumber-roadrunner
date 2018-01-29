package com.rationaleemotions.formatter;

import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;

public class ThreadSafeJson extends JSONFormatter {
    private static ThreadLocal<Boolean> inScenarioOutline = new InheritableThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public ThreadSafeJson(Appendable out) {
        super(out);
    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
        inScenarioOutline.set(true);
    }

    @Override
    public void examples(Examples examples) {
        // NoOp
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
        inScenarioOutline.set(false);
        super.startOfScenarioLifeCycle(scenario);
    }

    @Override
    public void step(Step step) {
        if (!inScenarioOutline.get()) {
            super.step(step);
        }
    }
}
