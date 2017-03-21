package io.limb.seabreakr;

import io.limb.seabreakr.spi.Context;
import io.limb.seabreakr.spi.EventListener;
import io.limb.seabreakr.spi.EventPublisher;
import io.limb.seabreakr.spi.Strategy;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static io.limb.seabreakr.BreakerExceptions.rethrow;
import static io.limb.seabreakr.BreakerExceptions.unwrapException;

class JavaProxyCircuitBreaker<T>
        extends AbstractCircuitBreaker<T>
        implements InvocationHandler {

    private final Strategy strategy;
    private final boolean callThrough;
    private final Scheduler scheduler;
    private final long timeout;

    JavaProxyCircuitBreaker(Class<T> type, Strategy strategy, T backend, T failover, long timeout,
                            EventListener listener, boolean callThrough, Scheduler scheduler) {

        super(type, backend, failover, listener, 1000, ContextImpl::new);

        this.strategy = strategy;
        this.timeout = timeout;
        this.scheduler = scheduler;
        this.callThrough = callThrough;
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
                .subscribeOn(scheduler)
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
}
