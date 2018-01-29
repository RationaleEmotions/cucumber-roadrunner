package com.rationaleemotions.internal.listeners;

import com.rationaleemotions.internal.RuntimeBehavior;
import com.rationaleemotions.internal.ThreadSafeObjectFactory;
import cucumber.api.java.ObjectFactory;
import org.testng.IExecutionListener;

public class ObjectFactoryInjector implements IExecutionListener {
    @Override
    public void onExecutionStart() {
        if (RuntimeBehavior.runScenariosInParallel()) {
            System.setProperty(ObjectFactory.class.getName(), ThreadSafeObjectFactory.class.getName());
        }
    }

    @Override
    public void onExecutionFinish() {

    }
}
