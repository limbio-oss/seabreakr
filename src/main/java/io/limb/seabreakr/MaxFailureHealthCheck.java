package io.limb.seabreakr;

import java.util.function.Predicate;

public class MaxFailureHealthCheck<T> implements Predicate<T> {

    private final int maxFailures;
    private int failures;

    public MaxFailureHealthCheck(int maxFailures) {
        this.maxFailures = maxFailures;
    }

    @Override
    public boolean test(T t) {
        return false;
    }
}
