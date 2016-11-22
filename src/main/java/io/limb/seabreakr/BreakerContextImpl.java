package io.limb.seabreakr;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

final class BreakerContextImpl
        implements BreakerContext {

    private static final AtomicReferenceFieldUpdater<BreakerContextImpl, BreakerState> STATE_UPDATER = //
            AtomicReferenceFieldUpdater.newUpdater(BreakerContextImpl.class, BreakerState.class, "state");

    private final BreakerMetricsRecorder metrics;
    private final BreakerEventPublisher eventPublisher;

    // Updated through field updater only
    private volatile BreakerState state = BreakerState.Closed;

    BreakerContextImpl(int numOfBufferedEvents, BreakerEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.metrics = new BreakerMetricsRecorderImpl(numOfBufferedEvents);
    }

    @Override
    public boolean close() {
        return updateStatus(BreakerState.Open, BreakerState.Closed);
    }

    @Override
    public boolean open() {
        return updateStatus(BreakerState.Closed, BreakerState.Open);
    }

    @Override
    public boolean isCallAllowed() {
        return state == BreakerState.Closed;
    }

    @Override
    public BreakerMetrics getMetrics() {
        return metrics;
    }

    @Override
    public BreakerMetricsRecorder getMetricsRecorder() {
        return metrics;
    }

    @Override
    public BreakerState getState() {
        return state;
    }

    @Override
    public BreakerEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    private boolean updateStatus(BreakerState oldStatus, BreakerState newStatus) {
        while (true) {
            BreakerState status = this.state;
            if (status == newStatus) {
                return false;
            }
            if (status != oldStatus) {
                throw new IllegalStateException("Expected state " + oldStatus + " but was " + status);
            }
            if (STATE_UPDATER.compareAndSet(this, status, newStatus)) {
                return true;
            }
        }
    }
}
