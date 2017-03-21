package io.limb.seabreakr;

import io.limb.seabreakr.spi.Context;
import io.limb.seabreakr.spi.EventPublisher;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

final class ContextImpl
        implements Context {

    private static final AtomicReferenceFieldUpdater<ContextImpl, State> STATE_UPDATER = //
            AtomicReferenceFieldUpdater.newUpdater(ContextImpl.class, State.class, "state");

    private final MetricsRecorder metrics;
    private final EventPublisher eventPublisher;

    // Updated through field updater only
    private volatile State state = State.Closed;

    ContextImpl(int numOfBufferedEvents, EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.metrics = new MetricsRecorderImpl(numOfBufferedEvents);
    }

    @Override
    public boolean close() {
        return updateStatus(State.Open, State.Closed);
    }

    @Override
    public boolean open() {
        return updateStatus(State.Closed, State.Open);
    }

    @Override
    public boolean isCallAllowed() {
        return state == State.Closed;
    }

    @Override
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public MetricsRecorder getMetricsRecorder() {
        return metrics;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventPublisher;
    }

    private boolean updateStatus(State oldStatus, State newStatus) {
        while (true) {
            State status = this.state;
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
