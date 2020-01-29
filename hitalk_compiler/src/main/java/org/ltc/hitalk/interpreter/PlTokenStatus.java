package org.ltc.hitalk.interpreter;

public enum PlTokenStatus {
    POLLED,
    PUSHED_BACK,
    ;

    private int times;

    PlTokenStatus() {
        increment();
    }

    public void increment() {
        ++times;
    }
}
