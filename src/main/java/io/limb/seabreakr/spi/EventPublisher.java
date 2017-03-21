package io.limb.seabreakr.spi;

public interface EventPublisher {

    void fireOpenState();

    void fireHalfOpenState();

    void fireClosedState();

}
