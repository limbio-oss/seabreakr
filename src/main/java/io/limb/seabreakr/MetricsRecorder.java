package io.limb.seabreakr;

public interface MetricsRecorder
        extends Metrics {

    void recordSuccess();

    void recordFailure();

    void reset();

}
