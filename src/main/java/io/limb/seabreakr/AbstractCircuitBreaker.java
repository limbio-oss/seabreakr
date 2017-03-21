package io.limb.seabreakr;

import io.limb.seabreakr.spi.Context;
import io.limb.seabreakr.spi.ContextFactory;
import io.limb.seabreakr.spi.EventListener;
import io.limb.seabreakr.spi.EventPublisher;

public abstract class AbstractCircuitBreaker<T>
        implements CircuitBreaker {

    protected final EventListener listener;
    protected final Context context;
    protected final Class<T> type;
    protected final T backend;
    protected final T failover;

    protected AbstractCircuitBreaker(Class<T> type, T backend, T failover, EventListener listener,
                                     int numOfBufferedEvents, ContextFactory contextFactory) {

        this.context = contextFactory.newContext(numOfBufferedEvents, eventPublisher);
        this.listener = listener;
        this.failover = failover;
        this.backend = backend;
        this.type = type;
    }

    @Override
    public void close() {
        context.close();
    }

    @Override
    public void open() {
        context.open();
    }

    @Override
    public boolean isCallAllowed() {
        return context.isCallAllowed();
    }

    @Override
    public State getState() {
        return context.getState();
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public Metrics getMetrics() {
        return context.getMetrics();
    }

    private final EventPublisher eventPublisher = new EventPublisher() {
        private final Event OPEN_STATE = new Event(State.Open, AbstractCircuitBreaker.this);
        private final Event HALF_OPEN_STATE = new Event(State.HalfOpen, AbstractCircuitBreaker.this);
        private final Event CLOSED_STATE = new Event(State.Closed, AbstractCircuitBreaker.this);

        @Override
        public void fireOpenState() {
            fireEvent(OPEN_STATE);
        }

        @Override
        public void fireHalfOpenState() {
            fireEvent(HALF_OPEN_STATE);
        }

        @Override
        public void fireClosedState() {
            fireEvent(CLOSED_STATE);
        }

        private void fireEvent(Event event) {
            if (listener != null) {
                listener.onEvent(event);
            }
        }
    };
}
