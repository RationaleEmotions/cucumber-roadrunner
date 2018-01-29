package com.rationaleemotions.formatter;

import cucumber.runtime.formatter.ColorAware;

public class ThreadSafePretty extends PrettyFormatter implements ColorAware {
    public ThreadSafePretty(Appendable out) {
        super(out, false, true);
    }

    @Override
    public void setMonochrome(boolean monochrome) {
        super.setMonochrome(monochrome);
    }
}
