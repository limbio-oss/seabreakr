package io.limb.seabreakr;

public interface BreakerStrategy {

    boolean isCallAllowed(BreakerContext context);

    void onFailure(BreakerContext context, BreakerEventListener listener);

    void onSuccess(BreakerContext context, BreakerEventListener listener);

    default void fireEvent(BreakerEventListener listener, BreakerState state, CircuitBreaker circuitBreaker) {

    }

}
