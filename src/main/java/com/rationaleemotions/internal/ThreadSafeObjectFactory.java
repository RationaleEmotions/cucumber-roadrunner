package com.rationaleemotions.internal;

import cucumber.api.java.ObjectFactory;
import cucumber.runtime.model.CucumberTagStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadSafeObjectFactory implements ObjectFactory {
    private Map<CucumberTagStatement, List<Mapping>> mapping = new HashMap<>();

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        mapping.remove(ScenarioDetailsManager.getCurrentScenarioDetails().getScenario());
    }

    @Override
    public boolean addClass(Class<?> glueClass) {
        return true;
    }

    @Override
    public <T> T getInstance(Class<T> glueClass) {
        try {
            List<Mapping> mappings = mapping.get(ScenarioDetailsManager.getCurrentScenarioDetails().getScenario());
            if (mappings == null) {
                mappings = new ArrayList<>();
                T instance = glueClass.getConstructor().newInstance();
                mappings.add(new Mapping(glueClass, instance));
                mapping.put(ScenarioDetailsManager.getCurrentScenarioDetails().getScenario(), mappings);
                return instance;
            }
            for (Mapping each : mappings) {
                if (each.getClazz().equals(glueClass)) {
                    return glueClass.cast(each.getObject());
                }
            }
            T instance = glueClass.getConstructor().newInstance();
            mappings.add(new Mapping(glueClass, instance));
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class Mapping {
        private Class<?> clazz;
        private Object object;

        Mapping(Class<?> clazz, Object object) {
            this.clazz = clazz;
            this.object = object;
        }

        Class<?> getClazz() {
            return clazz;
        }

        Object getObject() {
            return object;
        }
    }
}
