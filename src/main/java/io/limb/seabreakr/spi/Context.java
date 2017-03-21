package io.limb.seabreakr.spi;

import io.limb.seabreakr.Metrics;
import io.limb.seabreakr.MetricsRecorder;
import io.limb.seabreakr.State;

public interface Context {
    boolean close();

    boolean open();

    boolean isCallAllowed();

    Metrics getMetrics();

    MetricsRecorder getMetricsRecorder();

    State getState();

    EventPublisher getEventPublisher();

}
