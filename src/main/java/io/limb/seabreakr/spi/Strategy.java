package io.limb.seabreakr.spi;

import io.limb.seabreakr.State;
import io.limb.seabreakr.CircuitBreaker;

public interface Strategy {

    boolean isCallAllowed(Context context);

    void onFailure(Context context, EventListener listener);

    void onSuccess(Context context, EventListener listener);

    default void fireEvent(EventListener listener, State state, CircuitBreaker circuitBreaker) {

    }

}
