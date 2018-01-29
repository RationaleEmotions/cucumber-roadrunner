package com.rationaleemotions.internal;

import com.rationaleemotions.PluginMapper;
import com.rationaleemotions.formatter.ThreadSafeHtml;
import com.rationaleemotions.formatter.ThreadSafeJson;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that wires in the default thread-safe plugins into the execution model.
 */
public class DefaultPluginMapper implements PluginMapper {
    private static final Map<String, Class<?>> PLUGIN_CLASSES = new HashMap<>();

    static {
        PLUGIN_CLASSES.put("html", ThreadSafeHtml.class);
        PLUGIN_CLASSES.put("json", ThreadSafeJson.class);
//        PLUGIN_CLASSES.put("pretty", ThreadSafePretty.class);
//        PLUGIN_CLASSES.put("null", NullFormatter.class);
//        PLUGIN_CLASSES.put("junit", JUnitFormatter.class);
//        PLUGIN_CLASSES.put("testng", TestNGFormatter.class);
//        PLUGIN_CLASSES.put("progress", ProgressFormatter.class);
//        PLUGIN_CLASSES.put("usage", UsageFormatter.class);
//        PLUGIN_CLASSES.put("rerun", RerunFormatter.class);
//        PLUGIN_CLASSES.put("default_summary", DefaultSummaryPrinter.class);
//        PLUGIN_CLASSES.put("null_summary", NullSummaryPrinter.class);
    }

    @Override
    public Map<String, Class<?>> getMapping() {
        return PLUGIN_CLASSES;
    }
}
