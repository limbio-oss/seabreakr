package io.limb.seabreakr;

import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static io.limb.seabreakr.BreakerExceptions.rethrow;
import static io.limb.seabreakr.BreakerExceptions.unwrapException;

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

        Mono<Object> main;
        Mono<Object> fallback;

        boolean isMono = isMono(method);
        if (isMono) {
            main = invokeAsMono(method, args);
            fallback = invokeAsMono(method, failover, args);
        } else {
            CompletableFuture<Object> completableFuture = buildCompletableFuture(method, args);
            main = Mono.fromFuture(completableFuture);
            fallback = timeoutHandler(method, args);
        }

        Duration duration = Duration.ofNanos(timeout);
        Mono<?> mono = main
                .timeout(duration, fallback)
                .mapError(BreakerExceptions::unwrapException)
                .doOnTerminate(this::handleResult);

        if (isMono) {
            return mono;
        }
        return mono.block();
    }

    private <V> Mono<V> invokeAsMono(Method method, Object[] args) {
        Object proxy = isCallAllowed() ? backend : failover;
        return invokeAsMono(method, proxy, args);
    }

    private <V> Mono<V> invokeAsMono(Method method, Object proxy, Object[] args) {
        if (proxy == null) {
            return Mono.error(createNoSuchFailoverException());
        }
        ThrowingSupplier<Mono<V>> invoker = invoke(method, proxy, args);
        return invoker.get();
    }

    private NoSuchFailoverException createNoSuchFailoverException() {
        return new NoSuchFailoverException("Circuit breaker cannot execute, no failover available");
    }

    private <V> CompletableFuture<V> buildCompletableFuture(Method method, Object[] args) {
        Object proxy = isCallAllowed() ? backend : failover;
        ThrowingSupplier<V> invoker = invoke(method, proxy, args);
        return CompletableFuture.supplyAsync(invoker);
    }

    private <V> CompletableFuture<V> buildCompletableFuture(Method method, Object proxy, Object[] args) {
        ThrowingSupplier<V> invoker = invoke(method, proxy, args);
        return CompletableFuture.supplyAsync(invoker);
    }

    @SuppressWarnings("unchecked")
    private <V> ThrowingSupplier<V> invoke(Method method, Object proxy, Object[] args) {
        return () -> {
            try {
                return (V) method.invoke(proxy, args);
            } catch (Throwable throwable) {
                if (proxy == backend) {
                    context.getMetricsRecorder().recordFailure();
                }
                Throwable t = unwrapException(throwable);
                throw rethrow(t);
            }
        };
    }

    private <V> Mono<V> timeoutHandler(Method method, Object[] args) {
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
