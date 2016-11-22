package io.limb.seabreakr;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.LongBinaryOperator;

final class BitRing {

    private static final int NUM_BITS_PER_WORD = 64; // long = 64bits

    private final AtomicLongArray words;
    private final int bits;

    BitRing(int bits) {
        Preconditions.requireNonNegativeInt(bits, "bits must not be negative");
        this.words = words(bits);
        this.bits = this.words.length() * NUM_BITS_PER_WORD;
    }

    boolean get(long bit) {
        long word = words.get(index(bit));
        return (word & mask(bit)) != 0;
    }

    void set(long bit) {
        set(bit, (w, m) -> w | m);
    }

    void clear(long bit) {
        set(bit, (w, m) -> w & ~m);
    }

    void reset() {
        for (int i = 0; i < words.length(); i++) {
            words.set(i, 0);
        }
    }

    int cardinality() {
        int cardinality = 0;
        for (int i = 0; i < words.length(); i++) {
            cardinality += Long.bitCount(words.get(i));
        }
        return cardinality;
    }

    int size() {
        return bits;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(bits);
        for (int i = 0; i < words.length(); i++) {
            sb.append(Long.toBinaryString(words.get(i)));
        }
        return sb.toString();
    }

    private void set(long bit, LongBinaryOperator calculator) {
        int index = index(bit);
        long mask = mask(bit);
        while (true) {
            long oldWord = words.get(index);
            long newWord = calculator.applyAsLong(oldWord, mask);
            if (words.compareAndSet(index, oldWord, newWord)) {
                return;
            }
        }
    }

    private int index(long bit) {
        return (int) (bit / NUM_BITS_PER_WORD);
    }

    private int offset(long bit) {
        return (int) (bit % NUM_BITS_PER_WORD);
    }

    private long mask(long bit) {
        return 1L << offset(bit);
    }

    private AtomicLongArray words(int bits) {
        return new AtomicLongArray(new long[index(bits - 1) + 1]);
    }
}
