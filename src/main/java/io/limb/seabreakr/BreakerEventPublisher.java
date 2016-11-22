package io.limb.seabreakr;

public interface BreakerEventPublisher {

    void fireOpenState();

    void fireHalfOpenState();

    void fireClosedState();

}
