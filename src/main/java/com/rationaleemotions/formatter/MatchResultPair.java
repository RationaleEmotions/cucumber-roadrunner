package com.rationaleemotions.formatter;

import gherkin.formatter.Argument;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;

import java.util.Collections;
import java.util.List;

class MatchResultPair {
    private final Match match;
    public final Result result;

    MatchResultPair(Match match, Result result) {
        this.match = match;
        this.result = result;
    }

    public List<Argument> getMatchArguments() {
        if (match != null) {
            return match.getArguments();
        }
        return Collections.emptyList();
    }

    public String getMatchLocation() {
        if (match != null) {
            return match.getLocation();
        }
        return null;
    }

    public String getResultStatus() {
        if (result != null) {
            return result.getStatus();
        }
        return "skipped";
    }

    public boolean hasResultErrorMessage() {
        return result != null && result.getErrorMessage() != null;
    }
}
