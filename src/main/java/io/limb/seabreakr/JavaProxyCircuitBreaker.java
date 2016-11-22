package io.limb.seabreakr;

import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

class JavaProxyCircuitBreaker<T>
        implements CircuitBreaker, InvocationHandler {

    private final BreakerEventListener listener;
    private final BreakerContext context;
    private final BreakerStrategy strategy;
    private final boolean callThrough;
    private final Class<T> type;
    private final long timeout;
    private final T backend;
    private final T failover;

    JavaProxyCircuitBreaker(Class<T> type, BreakerStrategy strategy, T backend, //
                            T failover, long timeout, BreakerEventListener listener, boolean callThrough) {

        this.type = type;
        this.strategy = strategy;
        this.backend = backend;
        this.failover = failover;
        this.timeout = timeout;
        this.listener = listener;
        this.callThrough = callThrough;
        this.context = new BreakerContextImpl(1000, eventPublisher);
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
    public BreakerState getState() {
        return context.getState();
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public BreakerMetrics getMetrics() {
        return context.getMetrics();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!isCallAllowed() && failover == null) {
            throw createNoSuchFailoverException();
        }

        CompletableFuture<T> completableFuture = buildCompletableFuture(method, args);
        Mono<T> timeoutHandler = timeoutHandler(method, args);
        Duration duration = Duration.ofNanos(timeout);

        Mono<T> mono = Mono.fromFuture(completableFuture).timeout(duration, timeoutHandler)
                .doOnTerminate(this::handleResult).mapError(this::unwrapException);

        if (isMono(method)) {
            return mono;
        }
        return mono.block();
    }

    private NoSuchFailoverException createNoSuchFailoverException() {
        return new NoSuchFailoverException("Circuit breaker cannot execute, no failover available");
    }

    private Throwable unwrapException(Throwable throwable) {
        Throwable t = Exceptions.unwrap(throwable);
        return t instanceof InvocationTargetException ? t.getCause() : t;
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<T> buildCompletableFuture(Method method, Object[] args) {
        Object proxy = isCallAllowed() ? backend : failover;
        return buildCompletableFuture(method, proxy, args);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<T> buildCompletableFuture(Method method, Object proxy, Object[] args) {
        return CompletableFuture.supplyAsync((ThrowingSupplier<T>) () -> {
            try {
                return (T) method.invoke(proxy, args);
            } catch (Throwable throwable) {
                if (proxy == backend) {
                    context.getMetricsRecorder().recordFailure();
                }
                throw throwable;
            }
        });
    }

    private Mono<T> timeoutHandler(Method method, Object[] args) {
        if (callThrough) {
            if (failover == null) {
                throw createNoSuchFailoverException();
            }
            return Mono.fromFuture(buildCompletableFuture(method, failover, args));
        }

        return Mono.error(new CallTimeoutException("Call timed out"));
    }

    private boolean isMono(Method method) {
        return isReturnType(method, Mono.class);
    }

    private boolean isReturnType(Method method, Class<?> type) {
        return type.isAssignableFrom(method.getReturnType());
    }

    private void handleResult(Object value, Throwable error) {
        if (error != null) {
            strategy.onFailure(context, listener);
        } else {
            strategy.onSuccess(context, listener);
        }
    }

    private final BreakerEventPublisher eventPublisher = new BreakerEventPublisher() {
        private final BreakerEvent OPEN_STATE = new BreakerEvent(BreakerState.Open, JavaProxyCircuitBreaker.this);
        private final BreakerEvent HALF_OPEN_STATE = new BreakerEvent(BreakerState.HalfOpen, JavaProxyCircuitBreaker.this);
        private final BreakerEvent CLOSED_STATE = new BreakerEvent(BreakerState.Closed, JavaProxyCircuitBreaker.this);

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

        private void fireEvent(BreakerEvent event) {
            if (listener != null) {
                listener.onEvent(event);
            }
        }
    };
}
