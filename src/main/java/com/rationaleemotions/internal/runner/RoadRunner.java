package com.rationaleemotions.internal.runner;

import com.rationaleemotions.internal.PluginCreator;
import cucumber.api.testng.CucumberExceptionWrapper;
import cucumber.api.testng.CucumberFeatureWrapper;
import cucumber.api.testng.CucumberFeatureWrapperImpl;
import cucumber.api.testng.FeatureResultListener;
import cucumber.api.testng.TestNgReporter;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.CucumberException;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.Formatter;

import java.util.ArrayList;
import java.util.List;

public class RoadRunner {
    private Runtime runtime;
    private RuntimeOptions runtimeOptions;
    private ResourceLoader resourceLoader;
    private FeatureResultListener resultListener;
    private ClassLoader classLoader;


    /**
     * Bootstrap the cucumber runtime
     *
     * @param clazz Which has the cucumber.api.CucumberOptions and org.testng.annotations.Test annotations
     */
    public RoadRunner(Class clazz) {
        classLoader = clazz.getClassLoader();
        resourceLoader = new MultiLoader(classLoader);

        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz);
        runtimeOptions = runtimeOptionsFactory.create();
        List<Object> plugins = new PluginCreator().create(clazz);
        plugins.stream().forEach(plugin -> runtimeOptions.addPlugin(plugin));
        TestNgReporter reporter = new TestNgReporter(System.out);
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        resultListener = new FeatureResultListener(runtimeOptions.reporter(classLoader), runtimeOptions.isStrict());
        runtime = new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }

    /**
     * Run the Cucumber features
     */
    public void runCukes() {
        for (CucumberFeature cucumberFeature : getFeatures()) {
            AbstractRunner runner = FeatureRunnerFactory.newRunner(cucumberFeature);
            runner.run(runtimeOptions.formatter(classLoader), resultListener, runtime);
        }
        finish();
        if (!resultListener.isPassed()) {
            throw new CucumberException(resultListener.getFirstError());
        }
    }

    public void runCucumber(CucumberFeature cucumberFeature) {
        AbstractRunner runner = FeatureRunnerFactory.newRunner(cucumberFeature);
        runner.run(runtimeOptions.formatter(classLoader), resultListener, runtime);

        if (!resultListener.isPassed()) {
            throw new CucumberException(resultListener.getFirstError());
        }
    }

    public void finish() {
        Formatter formatter = runtimeOptions.formatter(classLoader);

        formatter.done();
        formatter.close();
        runtime.printSummary();
    }

    /**
     * @return List of detected cucumber features
     */
    public List<CucumberFeature> getFeatures() {
        return runtimeOptions.cucumberFeatures(resourceLoader);
    }

    /**
     * @return returns the cucumber features as a two dimensional array of
     * {@link CucumberFeatureWrapper} objects.
     */
    public Object[][] provideFeatures() {
        try {
            List<CucumberFeature> features = getFeatures();
            List<Object[]> featuresList = new ArrayList<>(features.size());
            for (CucumberFeature feature : features) {
                featuresList.add(new Object[]{new CucumberFeatureWrapperImpl(feature)});
            }
            return featuresList.toArray(new Object[][]{});
        } catch (CucumberException e) {
            return new Object[][]{new Object[]{new CucumberExceptionWrapper(e)}};
        }
    }
}
