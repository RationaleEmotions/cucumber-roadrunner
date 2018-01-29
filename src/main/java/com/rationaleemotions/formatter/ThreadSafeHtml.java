package com.rationaleemotions.formatter;

import com.rationaleemotions.internal.FeatureLogs;
import com.rationaleemotions.internal.ScenarioDetailsManager;
import cucumber.runtime.CucumberException;
import cucumber.runtime.io.URLOutputStream;
import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.GsonBuilder;
import gherkin.formatter.Formatter;
import gherkin.formatter.Mappable;
import gherkin.formatter.NiceAppendable;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ThreadSafeHtml implements Formatter, Reporter {

    private Map<Feature, FeatureLogs> mapping = new ConcurrentHashMap<>();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String JS_FORMATTER_VAR = "formatter";
    private static final String JS_REPORT_FILENAME = "report.js";
    private static final String[] TEXT_ASSETS = new String[]{"/cucumber/formatter/formatter.js", "/cucumber/formatter/index.html", "/cucumber/formatter/jquery-1.8.2.min.js", "/cucumber/formatter/style.css"};
    private static final Map<String, String> MIME_TYPES_EXTENSIONS = new HashMap<String, String>() {
        {
            put("image/bmp", "bmp");
            put("image/gif", "gif");
            put("image/jpeg", "jpg");
            put("image/png", "png");
            put("image/svg+xml", "svg");
            put("video/ogg", "ogg");
        }
    };

    private final URL htmlReportDir;
    private NiceAppendable jsOut;
    private final Object lock = new Object();

    private volatile boolean firstFeature = true;
    private int embeddedIndex;

    public ThreadSafeHtml(URL htmlReportDir) {
        this.htmlReportDir = htmlReportDir;
    }

    @Override
    public void uri(String uri) {
        Feature key = ScenarioDetailsManager.getCurrentScenarioDetails().getFeature();
        FeatureLogs data = new FeatureLogs();
        mapping.put(key, data);

        if (firstFeature) {
            synchronized (lock) {
                if (firstFeature) {
                    data.getAppender().append("$(document).ready(function() {").append("var ")
                            .append(JS_FORMATTER_VAR).append(" = new CucumberHTML.DOMFormatter($('.cucumber-report'));");
                    firstFeature = false;
                }
            }
        }
        String text = jsFunctionCall("uri", uri);
        data.getAppender().append(text);
    }

    @Override
    public void feature(Feature feature) {
        String text = jsFunctionCall("feature", feature);
        mapping.get(feature).getAppender().append(text);
    }

    @Override
    public void background(Background background) {
        String text = jsFunctionCall("background", background);
        writeDetails(text);
    }

    @Override
    public void scenario(Scenario scenario) {
        String text = jsFunctionCall("scenario", scenario);
        writeDetails(text);
    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
        String text = jsFunctionCall("scenarioOutline", scenarioOutline);
        writeDetails(text);
    }

    @Override
    public void examples(Examples examples) {
        String text = jsFunctionCall("examples", examples);
        writeDetails(text);
    }

    @Override
    public void step(Step step) {
        String text = jsFunctionCall("step", step);
        writeDetails(text);
    }

    @Override
    public void eof() {

    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
    }

    @Override
    public void done() {
        if (!firstFeature) {
            Feature key = ScenarioDetailsManager.getCurrentScenarioDetails().getFeature();
            FeatureLogs allScenarios = mapping.get(key);
            jsOut().append(allScenarios.getAppender().toString());
            for(List<String> data : allScenarios.getScenarioLogs().values()) {
                String text = data.stream().collect(Collectors.joining());
                jsOut().append(text);
            }
            jsOut().append("});");
            copyReportFiles();
        }
    }

    @Override
    public void close() {
        jsOut().close();
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
        // NoOp
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        // NoOp
    }

    @Override
    public void result(Result result) {
        String text = jsFunctionCall("result", result);
        writeDetails(text);
    }

    @Override
    public void before(Match match, Result result) {
        String text = jsFunctionCall("before", result);
        writeDetails(text);
    }

    @Override
    public void after(Match match, Result result) {
        String text = jsFunctionCall("after", result);
        writeDetails(text);
    }

    @Override
    public void match(Match match) {
        String text = jsFunctionCall("match", match);
        writeDetails(text);
    }

    @Override
    public void embedding(String mimeType, byte[] data) {
        if(mimeType.startsWith("text/")) {
            // just pass straight to the formatter to output in the html
            String text = jsFunctionCall("embedding", mimeType, new String(data));
            writeDetails(text);
        } else {
            // Creating a file instead of using data urls to not clutter the js file
            String extension = MIME_TYPES_EXTENSIONS.get(mimeType);
            if (extension != null) {
                StringBuilder fileName = new StringBuilder("embedded").append(embeddedIndex++).append(".").append(extension);
                writeBytesAndClose(data, reportFileOutputStream(fileName.toString()));
                String text = jsFunctionCall("embedding", mimeType, fileName);
                writeDetails(text);
            }
        }
    }

    @Override
    public void write(String text) {
        String text1 = jsFunctionCall("write", text);
        writeDetails(text1);
    }

    private void writeDetails(String text) {
        Utils.writeDetails(text, mapping);
    }

    private String jsFunctionCall(String functionName, Object... args) {
        StringBuilder out = new StringBuilder();
        out.append(JS_FORMATTER_VAR + ".").append(functionName).append("(");
        boolean comma = false;
        for (Object arg : args) {
            if (comma) {
                out.append(", ");
            }
            arg = arg instanceof Mappable ? ((Mappable) arg).toMap() : arg;
            String stringArg = gson.toJson(arg);
            out.append(stringArg);
            comma = true;
        }
        out.append(");");
        return out.toString();
    }

    private void copyReportFiles() {
        for (String textAsset : TEXT_ASSETS) {
            InputStream textAssetStream = getClass().getResourceAsStream(textAsset);
            if (textAssetStream == null) {
                throw new CucumberException("Couldn't find " + textAsset + ". Is cucumber-html on your classpath? Make sure you have the right version.");
            }
            String baseName = new File(textAsset).getName();
            writeStreamAndClose(textAssetStream, reportFileOutputStream(baseName));
        }
    }

    private void writeStreamAndClose(InputStream in, OutputStream out) {
        byte[] buffer = new byte[16 * 1024];
        try {
            int len = in.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = in.read(buffer);
            }
            out.close();
        } catch (IOException e) {
            throw new CucumberException("Unable to write to report file item: ", e);
        }
    }

    private void writeBytesAndClose(byte[] buf, OutputStream out) {
        try {
            out.write(buf);
        } catch (IOException e) {
            throw new CucumberException("Unable to write to report file item: ", e);
        }
    }

    private NiceAppendable jsOut() {
        if (jsOut == null) {
            try {
                jsOut = new NiceAppendable(new OutputStreamWriter(reportFileOutputStream(JS_REPORT_FILENAME), "UTF-8"));
            } catch (IOException e) {
                throw new CucumberException(e);
            }
        }
        return jsOut;
    }

    private OutputStream reportFileOutputStream(String fileName) {
        try {
            return new URLOutputStream(new URL(htmlReportDir, fileName));
        } catch (IOException e) {
            throw new CucumberException(e);
        }
    }

}