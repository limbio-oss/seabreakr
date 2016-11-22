package io.limb.seabreakr;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

final class BreakerMetricsRecorderImpl
        implements BreakerMetricsRecorder {

    private static final AtomicLongFieldUpdater<BreakerMetricsRecorderImpl> POSITION_UPDATER = //
            AtomicLongFieldUpdater.newUpdater(BreakerMetricsRecorderImpl.class, "position");

    private final BitRing bitRing;
    private final int numOfBufferedEvents;

    // Only updated through Atomic Updater
    private volatile long position = 0;

    BreakerMetricsRecorderImpl(int numOfBufferedEvents) {
        this.bitRing = new BitRing(numOfBufferedEvents);
        this.numOfBufferedEvents = this.bitRing.size();
    }

    @Override
    public float getFailureRate() {
        return getNumberOfFailedEvents() * 100.f / getNumberOfBufferedEvents();
    }

    @Override
    public int getNumberOfFailedEvents() {
        return bitRing.cardinality();
    }

    @Override
    public int getNumberOfBufferedEvents() {
        return numOfBufferedEvents;
    }

    @Override
    public void recordSuccess() {
        long position = nextPosition();
        bitRing.clear(position);
    }

    @Override
    public void recordFailure() {
        long position = nextPosition();
        bitRing.set(position);
    }

    @Override
    public void reset() {
        bitRing.reset();
        POSITION_UPDATER.set(this, 0);
    }

    String dump() {
        return bitRing.toString();
    }

    private long nextPosition() {
        while (true) {
            long currentPosition = this.position;
            long newPosition = currentPosition + 1;

            // Fix overflow
            if (newPosition < 0) {
                newPosition = 0;
            }

            if (POSITION_UPDATER.compareAndSet(this, currentPosition, newPosition)) {
                return newPosition;
            }
        }
    }
}
