package io.limb.seabreakr;

public interface BreakerContext {
    boolean close();

    boolean open();

    boolean isCallAllowed();

    BreakerMetrics getMetrics();

    BreakerMetricsRecorder getMetricsRecorder();

    BreakerState getState();

    BreakerEventPublisher getEventPublisher();

}
