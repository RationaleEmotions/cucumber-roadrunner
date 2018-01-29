package com.rationaleemotions.formatter;

import com.rationaleemotions.internal.FeatureLogs;
import com.rationaleemotions.internal.RuntimeBehavior;
import com.rationaleemotions.internal.ScenarioDetailsManager;
import gherkin.formatter.AnsiFormats;
import gherkin.formatter.Argument;
import gherkin.formatter.Format;
import gherkin.formatter.Formats;
import gherkin.formatter.Formatter;
import gherkin.formatter.MonochromeFormats;
import gherkin.formatter.NiceAppendable;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.BasicStatement;
import gherkin.formatter.model.CellResult;
import gherkin.formatter.model.Comment;
import gherkin.formatter.model.DocString;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Row;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import gherkin.formatter.model.Tag;
import gherkin.formatter.model.TagStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static gherkin.util.FixJava.join;
import static gherkin.util.FixJava.map;

/**
 * This class pretty prints feature files like they were in the source, only
 * prettier. That is, with consistent indentation. This class is also a {@link Reporter},
 * which means it can be used to print execution results - highlighting arguments,
 * printing source information and exception information.
 */
class PrettyFormatter implements Reporter, Formatter {
    private Map<Feature, FeatureLogs> mapping = new ConcurrentHashMap<>();
    private static final ThreadLocal<DataContainer> data =
            new InheritableThreadLocal<DataContainer>() {
                @Override
                protected DataContainer initialValue() {
                    return new DataContainer();
                }
            };
    private final NiceAppendable out;
    private final boolean executing;
    private Formats formats;
    private String uri;

    PrettyFormatter(Appendable out, boolean monochrome, boolean executing) {
        this.out = new NiceAppendable(out);
        this.executing = executing;
        setMonochrome(monochrome);
    }

    public void setMonochrome(boolean monochrome) {
        if (monochrome) {
            formats = new MonochromeFormats();
        } else {
            formats = new AnsiFormats();
        }
    }

    @Override
    public void uri(String uri) {
        this.uri = uri;
        Feature key = ScenarioDetailsManager.getCurrentScenarioDetails().getFeature();
        FeatureLogs data = new FeatureLogs();
        mapping.put(key, data);
    }

    @Override
    public void feature(Feature feature) {
        String text = printComments(feature.getComments(), "");
        mapping.get(feature).getAppender().append(text);
        text = printTags(feature.getTags(), "");
        mapping.get(feature).getAppender().append(text);
        text = feature.getKeyword() + ": " + feature.getName() + "\n";
        mapping.get(feature).getAppender().append(text);
        text = printDescription(feature.getDescription(), "  ", false);
        mapping.get(feature).getAppender().append(text);
    }

    @Override
    public void background(Background background) {
        data.get().statement = background;
        replay();
    }

    @Override
    public void scenario(Scenario scenario) {
        data.get().statement = scenario;
        replay();
    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
        data.get().statement = scenarioOutline;
        replay();
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
        // NoOp
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        if (RuntimeBehavior.runScenariosInParallel()) {
            printSteps();
        }
    }

    private void replay() {
        addAnyOrphanMatch();
        printStatement();
        printSteps();
    }

    private void printSteps() {
        while (!data.get().steps.isEmpty()) {
            if (data.get().matchesAndResults.isEmpty()) {
                printStep("skipped", Collections.emptyList(), null);
            } else {
                MatchResultPair matchAndResult = data.get().matchesAndResults.remove(0);
                printStep(matchAndResult.getResultStatus(), matchAndResult.getMatchArguments(),
                        matchAndResult.getMatchLocation());
                if (matchAndResult.hasResultErrorMessage()) {
                    printError(matchAndResult.result);
                }
            }
        }
    }

    private void printStatement() {
        DataContainer container = data.get();
        if (container.statement == null) {
            return;
        }
        calculateLocationIndentations();
        writeDetails("\n");
        String text = printComments(container.statement.getComments(), "  ");
        writeDetails(text);
        if (container.statement instanceof TagStatement) {
            text = printTags(((TagStatement) container.statement).getTags(), "  ");
            writeDetails(text);
        }
        StringBuilder buffer = new StringBuilder("  ");
        buffer.append(container.statement.getKeyword());
        buffer.append(": ");
        buffer.append(container.statement.getName());
        String location = executing ? uri + ":" + container.statement.getLine() : null;
        buffer.append(indentedLocation(location));
        buffer.append("\n");
        writeDetails(buffer.toString());

        if (container.statement != null) {
            text = printDescription(container.statement.getDescription(), "    ", true);
            writeDetails(text);
        }
        if (container.statement instanceof Scenario) {
//            data.get().statement = null;
        }
    }

    private void writeDetails(String text) {
        Utils.writeDetails(text, mapping);
    }

    private String indentedLocation(String location) {
        StringBuilder sb = new StringBuilder();
        List<Integer> indents = data.get().indentations;
        int indentation = indents.isEmpty() ? 0 : indents.remove(0);
        if (location == null) {
            return "";
        }
        for (int i = 0; i < indentation; i++) {
            sb.append(' ');
        }
        sb.append(' ');
        sb.append(getFormat("comment").text("# " + location));
        return sb.toString();
    }

    @Override
    public void examples(Examples examples) {
        replay();
        writeDetails("\n");
        String text = printComments(examples.getComments(), "    ");
        writeDetails(text);
        text = printTags(examples.getTags(), "    ");
        writeDetails(text);
        writeDetails("    " + examples.getKeyword() + ": " + examples.getName());
        writeDetails("\n");
        text = printDescription(examples.getDescription(), "      ", true);
        writeDetails(text);
        table(examples.getRows());
    }

    @Override
    public void step(Step step) {
        data.get().steps.add(step);
    }

    @Override
    public void match(Match match) {
        addAnyOrphanMatch();
        data.get().match = match;
    }

    private void addAnyOrphanMatch() {
        if (data.get().match != null) {
            data.get().matchesAndResults.add(new MatchResultPair(data.get().match, null));
        }
    }

    @Override
    public void embedding(String mimeType, byte[] data) {
        // Do nothing
    }

    @Override
    public void write(String text) {
        writeDetails(getFormat("output").text(text));
        writeDetails("\n");
    }

    @Override
    public void result(Result result) {
        data.get().matchesAndResults.add(new MatchResultPair(data.get().match, result));
        data.get().match = null;
    }

    @Override
    public void before(Match match, Result result) {
        printHookFailure(match, result, true);
    }

    @Override
    public void after(Match match, Result result) {
        printHookFailure(match, result, false);
    }

    private void printHookFailure(Match match, Result result, boolean isBefore) {
        if (result.getStatus().equals(Result.FAILED)) {
            Format format = getFormat(result.getStatus());

            StringBuilder context = new StringBuilder("Failure in ");
            if (isBefore) {
                context.append("before");
            } else {
                context.append("after");
            }
            context.append(" hook:");

            writeDetails(format.text(context.toString()) + format.text(match.getLocation()));
            writeDetails(format.text("Message: ") + format.text(result.getErrorMessage()));

            if (result.getError() != null) {
                printError(result);
            }
        }

    }

    private void printStep(String status, List<Argument> arguments, String location) {
        Iterator<Step> s = data.get().steps.iterator();
        Step step = s.next();
        s.remove();
        Format textFormat = getFormat(status);
        Format argFormat = getArgFormat(status);

        String text = printComments(step.getComments(), "    ");
        writeDetails(text);

        StringBuilder buffer = new StringBuilder("    ");
        buffer.append(textFormat.text(step.getKeyword()));
        data.get().stepPrinter.writeStep(new NiceAppendable(buffer), textFormat, argFormat, step.getName(), arguments);
        buffer.append(indentedLocation(location));

        writeDetails(buffer.toString());
        writeDetails("\n");
        if (step.getRows() != null) {
            table(step.getRows());
        } else if (step.getDocString() != null) {
            docString(step.getDocString());
        }
    }

    private Format getFormat(String key) {
        return formats.get(key);
    }

    private Format getArgFormat(String key) {
        return formats.get(key + "_arg");
    }

    private void table(List<? extends Row> rows) {
        prepareTable(rows);
        if (!executing) {
            for (Row row : rows) {
                row(row.createResults("skipped"));
                nextRow();
            }
        }
    }

    private void prepareTable(List<? extends Row> rows) {
        data.get().rows = rows;

        // find the largest row
        int columnCount = 0;
        for (Row row : rows) {
            if (columnCount < row.getCells().size()) {
                columnCount = row.getCells().size();
            }
        }

        data.get().cellLengths = new int[rows.size()][columnCount];
        data.get().maxLengths = new int[columnCount];
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = rows.get(rowIndex);
            final List<String> cells = row.getCells();
            for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                final String cell = getCellSafely(cells, colIndex);
                final int length = escapeCell(cell).length();
                data.get().cellLengths[rowIndex][colIndex] = length;
                data.get().maxLengths[colIndex] = Math.max(data.get().maxLengths[colIndex], length);
            }
        }
        data.get().rowIndex = 0;
    }

    private String getCellSafely(final List<String> cells, final int colIndex) {
        return (colIndex < cells.size()) ? cells.get(colIndex) : "";
    }

    private void row(List<CellResult> cellResults) {
        StringBuilder buffer = new StringBuilder();
        Row row = data.get().rows.get(data.get().rowIndex);
        if (data.get().rowsAbove) {
            buffer.append(formats.up(data.get().rowHeight));
        } else {
            data.get().rowsAbove = true;
        }
        data.get().rowHeight = 1;

        for (Comment comment : row.getComments()) {
            buffer.append("      ");
            buffer.append(comment.getValue());
            buffer.append("\n");
            data.get().rowHeight++;
        }
        switch (row.getDiffType()) {
            case NONE:
                buffer.append("      | ");
                break;
            case DELETE:
                buffer.append("    ").append(formats.get("skipped").text("-")).append(" | ");
                break;
            case INSERT:
                buffer.append("    ").append(formats.get("comment").text("+")).append(" | ");
                break;
        }
        for (int colIndex = 0; colIndex < data.get().maxLengths.length; colIndex++) {
            String cellText = escapeCell(getCellSafely(row.getCells(), colIndex));
            String status = null;
            switch (row.getDiffType()) {
                case NONE:
                    status = cellResults.size() < colIndex ? cellResults.get(colIndex).getStatus() : "skipped";
                    break;
                case DELETE:
                    status = "skipped";
                    break;
                case INSERT:
                    status = "comment";
                    break;
            }
            Format format = formats.get(status);
            buffer.append(format.text(cellText));
            int padding = data.get().maxLengths[colIndex] - data.get().cellLengths[data.get().rowIndex][colIndex];
            padSpace(buffer, padding);
            if (colIndex < data.get().maxLengths.length - 1) {
                buffer.append(" | ");
            } else {
                buffer.append(" |");
            }
        }
        writeDetails(buffer.toString());
        writeDetails("\n");
        data.get().rowHeight++;
        Set<Result> seenResults = new HashSet<>();
        for (CellResult cellResult : cellResults) {
            for (Result result : cellResult.getResults()) {
                if (result.getErrorMessage() != null && !seenResults.contains(result)) {
                    printError(result);
                    data.get().rowHeight += result.getErrorMessage().split("\n").length;
                    seenResults.add(result);
                }
            }
        }
    }

    private void printError(Result result) {
        Format failed = formats.get("failed");
        writeDetails(indent(failed.text(result.getErrorMessage()), "      "));
    }

    private void nextRow() {
        data.get().rowIndex++;
        data.get().rowsAbove = false;
    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void done() {
        // We're *not* closing the stream here.
        // https://github.com/cucumber/gherkin/issues/151
        // https://github.com/cucumber/cucumber-jvm/issues/96
    }

    @Override
    public void close() {
        Feature key = ScenarioDetailsManager.getCurrentScenarioDetails().getFeature();
        FeatureLogs allScenarios = mapping.get(key);
        out.append(allScenarios.getAppender().toString());
        for(List<String> data : allScenarios.getScenarioLogs().values()) {
            String text = data.stream().collect(Collectors.joining());
            out.append(text);
        }
        out.close();
    }

    private String escapeCell(String cell) {
        return cell.replaceAll("\\\\(?!\\|)", "\\\\\\\\")
                .replaceAll("\\n", "\\\\n")
                .replaceAll("\\|", "\\\\|");
    }

    private void docString(DocString docString) {
        writeDetails("      \"\"\"");
        writeDetails("\n");
        writeDetails(escapeTripleQuotes(indent(docString.getValue(), "      ")));
        writeDetails("\n");
        writeDetails("      \"\"\"");
        writeDetails("\n");
    }

    public void eof() {
        replay();
    }

    private void calculateLocationIndentations() {
        int[] lineWidths = new int[data.get().steps.size() + 1];
        int i = 0;

        List<BasicStatement> statements = new ArrayList<>();
        statements.add(data.get().statement);
        statements.addAll(data.get().steps);
        int maxLineWidth = 0;
        for (BasicStatement statement : statements) {
            int stepWidth = statement.getKeyword().length() + statement.getName().length();
            lineWidths[i++] = stepWidth;
            maxLineWidth = Math.max(maxLineWidth, stepWidth);
        }
        for (int lineWidth : lineWidths) {
            data.get().indentations.add(maxLineWidth - lineWidth);
        }
    }

    private void padSpace(StringBuilder buffer, int indent) {
        for (int i = 0; i < indent; i++) {
            buffer.append(" ");
        }
    }

    private String printComments(List<Comment> comments, String indent) {
        StringBuilder text = new StringBuilder();
        for (Comment comment : comments) {
            text.append(indent).append(comment.getValue()).append("\n");
        }
        return text.toString();
    }

    private String printTags(List<Tag> tags, String indent) {
        if (tags.isEmpty()) {
            return "";
        }
        return indent + join(map(tags, data.get().tagNameMapper), " ");
    }

    private String printDescription(String description, String indentation, boolean newline) {
        StringBuilder builder = new StringBuilder();
        if (!"".equals(description)) {
            builder.append(indent(description, indentation)).append("\n");
            if (newline) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private static final Pattern START = Pattern.compile("^", Pattern.MULTILINE);

    private static String indent(String s, String indentation) {
        return START.matcher(s).replaceAll(indentation);
    }

    private static final Pattern TRIPLE_QUOTES = Pattern.compile("\"\"\"", Pattern.MULTILINE);
    private static final String ESCAPED_TRIPLE_QUOTES = "\\\\\"\\\\\"\\\\\"";

    private static String escapeTripleQuotes(String s) {
        return TRIPLE_QUOTES.matcher(s).replaceAll(ESCAPED_TRIPLE_QUOTES);
    }
}
