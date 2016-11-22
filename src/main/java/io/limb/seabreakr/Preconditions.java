package io.limb.seabreakr;

final class Preconditions {

    private Preconditions() {
    }

    static void requireGreaterEquals(long value, long min, String msg) {
        if (value < min) {
            throw new IllegalArgumentException(msg);
        }
    }

    static void requireNonNegativeInt(int value, String msg) {
        if (value < 0) {
            throw new IllegalArgumentException(msg);
        }
    }
}
