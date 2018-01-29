package com.rationaleemotions.internal;

import com.rationaleemotions.PluginMapper;
import com.rationaleemotions.Reporters;
import cucumber.runtime.CucumberException;
import cucumber.runtime.io.URLOutputStream;
import cucumber.runtime.io.UTF8OutputStreamWriter;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cucumber.runtime.Utils.toURL;
import static java.util.Arrays.asList;

/**
 *
 */
public class PluginCreator {
    private final Class[] CTOR_ARGS = new Class[]{null, Appendable.class, URI.class, URL.class, File.class};
    private static final Map<String, Class<?>> PLUGIN_CLASSES = new HashMap<>();

    static {
        init();
    }

    private static void init() {
        ServiceLoader<PluginMapper> values = ServiceLoader.load(PluginMapper.class);
        values.forEach(provider -> provider.getMapping().forEach((k, v) -> {
            if (isFormatter(v)) {
                PLUGIN_CLASSES.put(k, v);
            }
        }));
    }

    private static final Pattern PLUGIN_WITH_FILE_PATTERN = Pattern.compile("([^:]+):(.*)");
    private String defaultOutFormatter = null;

    private Appendable defaultOut = new PrintStream(System.out) {
        @Override
        public void close() {
            // We have no intention to close System.out
        }
    };

    private Object create(String pluginString) {
        Matcher pluginWithFile = PLUGIN_WITH_FILE_PATTERN.matcher(pluginString);
        String pluginName;
        String path = null;
        if (pluginWithFile.matches()) {
            pluginName = pluginWithFile.group(1);
            path = pluginWithFile.group(2);
        } else {
            pluginName = pluginString;
        }
        Class<?> pluginClass = pluginClass(pluginName);
        try {
            return instantiate(pluginString, pluginClass, path);
        } catch (IOException | URISyntaxException e) {
            throw new CucumberException(e);
        }
    }

    public List<Object> create(Class<?> clazz) {
        List<Object> pluginObject = new ArrayList<>();
        List<String> plugins = new ArrayList<>();
        for (Class classWithOptions = clazz; hasSuperClass(classWithOptions); classWithOptions = classWithOptions.getSuperclass()) {
            Reporters options = getOptions(classWithOptions);
            if (options != null) {
                plugins.addAll(Arrays.asList(options.plugin()));
            }
        }
        plugins.forEach(plugin -> pluginObject.add(create(plugin)));
        return pluginObject;
    }

//    private List<String> addPlugins(CucumberOptions options ) {
//        List<String> args = new ArrayList<>();
//        List<String> plugins = new ArrayList<>();
//        plugins.addAll(asList(options.plugin()));
//        plugins.addAll(asList(options.format()));
//        for (String plugin : plugins) {
//            args.add("--plugin");
//            args.add(plugin);
//            if (isFormatterName(plugin)) {
//                pluginSpecified = true;
//            }
//        }
//        return args;
//    }

    private static boolean isFormatter(Class<?> pluginClass) {
        return Formatter.class.isAssignableFrom(pluginClass) || Reporter.class.isAssignableFrom(pluginClass);
    }

//    private static boolean isFormatterName(String name) {
//        Class pluginClass = getPluginClass(name);
//        return isFormatter(pluginClass);
//    }

//    private static Class getPluginClass(String name) {
//        Matcher pluginWithFile = PLUGIN_WITH_FILE_PATTERN.matcher(name);
//        String pluginName;
//        if (pluginWithFile.matches()) {
//            pluginName = pluginWithFile.group(1);
//        } else {
//            pluginName = name;
//        }
//        return pluginClass(pluginName);
//    }


    private Reporters getOptions(Class<?> clazz) {
        return clazz.getAnnotation(Reporters.class);
    }

    private boolean hasSuperClass(Class classWithOptions) {
        return classWithOptions != Object.class;
    }

    private <T> T instantiate(String pluginString, Class<T> pluginClass, String pathOrUrl) throws IOException, URISyntaxException {
        for (Class ctorArgClass : CTOR_ARGS) {
            Constructor<T> constructor = findConstructor(pluginClass, ctorArgClass);
            if (constructor != null) {
                Object ctorArg = convertOrNull(pathOrUrl, ctorArgClass, pluginString);
                try {
                    if (ctorArgClass == null) {
                        return constructor.newInstance();
                    } else {
                        if (ctorArg == null) {
                            throw new CucumberException(String.format("You must supply an output argument to %s. Like so: %s:output", pluginString, pluginString));
                        }
                        return constructor.newInstance(ctorArg);
                    }
                } catch (InstantiationException | IllegalAccessException  e) {
                    throw new CucumberException(e);
                } catch (InvocationTargetException e) {
                    throw new CucumberException(e.getTargetException());
                }
            }
        }
        throw new CucumberException(String.format("%s must have a constructor that is either empty or a single arg of one of: %s", pluginClass, asList(CTOR_ARGS)));
    }

    private Object convertOrNull(String pathOrUrl, Class ctorArgClass, String formatterString) throws IOException, URISyntaxException {
        if (ctorArgClass == null) {
            return null;
        }
        if (ctorArgClass.equals(URI.class)) {
            if (pathOrUrl != null) {
                return new URI(pathOrUrl);
            }
        }
        if (ctorArgClass.equals(URL.class)) {
            if (pathOrUrl != null) {
                return toURL(pathOrUrl);
            }
        }
        if (ctorArgClass.equals(File.class)) {
            if (pathOrUrl != null) {
                return new File(pathOrUrl);
            }
        }
        if (ctorArgClass.equals(Appendable.class)) {
            if (pathOrUrl != null) {
                return new UTF8OutputStreamWriter(new URLOutputStream(toURL(pathOrUrl)));
            } else {
                return defaultOutOrFailIfAlreadyUsed(formatterString);
            }
        }
        return null;
    }

    private <T> Constructor<T> findConstructor(Class<T> pluginClass, Class<?> ctorArgClass) {
        try {
            if (ctorArgClass == null) {
                return pluginClass.getConstructor();
            } else {
                return pluginClass.getConstructor(ctorArgClass);
            }
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Class<?> pluginClass(String pluginName) {
        Class<?> pluginClass = PLUGIN_CLASSES.get(pluginName);
        if (pluginClass == null) {
            pluginClass = loadClass(pluginName);
            if (!isFormatter(pluginClass)) {
                String msg = String.format("%s does not implement either %s (or) %s.", pluginClass.getName(),
                        Formatter.class.getName(), Reporter.class.getName());
                throw new IllegalArgumentException(msg);
            }
        }
        return pluginClass;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new CucumberException("Couldn't load plugin class: " + className, e);
        }
    }

    private Appendable defaultOutOrFailIfAlreadyUsed(String formatterString) {
        try {
            if (defaultOut != null) {
                defaultOutFormatter = formatterString;
                return defaultOut;
            } else {
                throw new CucumberException("Only one formatter can use STDOUT, now both " +
                        defaultOutFormatter + " and " + formatterString + " use it. " +
                        "If you use more than one formatter you must specify output path with PLUGIN:PATH_OR_URL");
            }
        } finally {
            defaultOut = null;
        }
    }
}
