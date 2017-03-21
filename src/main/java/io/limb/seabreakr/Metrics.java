package io.limb.seabreakr;

public interface Metrics {

    float getFailureRate();

    long getNumberOfFailedEvents();

    long getNumberOfBufferedEvents();

}
