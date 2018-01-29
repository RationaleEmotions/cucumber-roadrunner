package com.rationaleemotions.formatter;

import gherkin.formatter.StepPrinter;
import gherkin.formatter.model.DescribedStatement;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Row;
import gherkin.formatter.model.Step;
import gherkin.formatter.model.Tag;
import gherkin.util.Mapper;

import java.util.ArrayList;
import java.util.List;

public class DataContainer {
    StepPrinter stepPrinter = new StepPrinter();

    Mapper<Tag, String> tagNameMapper = Tag::getName;
    Match match;
    int[][] cellLengths;
    int[] maxLengths;
    int rowIndex;
    List<? extends Row> rows;
    Integer rowHeight = null;
    boolean rowsAbove = false;

    List<Step> steps = new ArrayList<>();
    List<Integer> indentations = new ArrayList<>();
    List<MatchResultPair> matchesAndResults = new ArrayList<>();
    DescribedStatement statement;
}
