package com.rationaleemotions;

import com.rationaleemotions.internal.runner.RoadRunner;
import cucumber.api.testng.CucumberFeatureWrapper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public abstract class TestNGCucumberTests {
    private RoadRunner runner;

    @BeforeClass(alwaysRun = true)
    public void setUpClass() {
        runner = new RoadRunner(this.getClass());
    }

    @Test(groups = "cucumber", description = "Runs Cucumber Feature", dataProvider = "features")
    public void feature(CucumberFeatureWrapper cucumberFeature) {
        runner.runCucumber(cucumberFeature.getCucumberFeature());
    }

    /**
     * @return returns two dimensional array of {@link CucumberFeatureWrapper} objects.
     */
    @DataProvider
    public Object[][] features() {
        return runner.provideFeatures();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        runner.finish();
    }
}
