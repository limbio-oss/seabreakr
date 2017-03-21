package io.limb.seabreakr;

public final class Event {

    private final State state;
    private final CircuitBreaker circuitBreaker;

    Event(State state, CircuitBreaker circuitBreaker) {
        this.state = state;
        this.circuitBreaker = circuitBreaker;
    }

    public State getState() {
        return state;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event that = (Event) o;

        if (state != that.state) return false;
        return circuitBreaker != null ? circuitBreaker.equals(that.circuitBreaker) : that.circuitBreaker == null;

    }

    @Override
    public int hashCode() {
        int result = state != null ? state.hashCode() : 0;
        result = 31 * result + (circuitBreaker != null ? circuitBreaker.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BreakerEvent{" + "state=" + state + ", circuitBreaker=" + circuitBreaker + '}';
    }
}
