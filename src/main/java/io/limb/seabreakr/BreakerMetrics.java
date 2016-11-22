package io.limb.seabreakr;

public interface BreakerMetrics {

    float getFailureRate();

    int getNumberOfFailedEvents();

    int getNumberOfBufferedEvents();

}
