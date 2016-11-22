package io.limb.seabreakr;

public interface BreakerMetricsRecorder
        extends BreakerMetrics {

    void recordSuccess();

    void recordFailure();

    void reset();

}
