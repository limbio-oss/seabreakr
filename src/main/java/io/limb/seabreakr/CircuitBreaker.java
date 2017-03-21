package io.limb.seabreakr;

import io.limb.seabreakr.spi.Strategy;

import java.lang.reflect.Proxy;

public interface CircuitBreaker {

    void close();

    void open();

    boolean isCallAllowed();

    State getState();

    Class<?> getType();

    Metrics getMetrics();

    static <T> CircuitBreakerBuilder<T> newBuilder(ServiceType<T> serviceType) {
        return new CircuitBreakerBuilderImpl<>(serviceType);
    }

    static <T> CircuitBreakerBuilder<T> newBuilder(ServiceType<T> serviceType, Strategy strategy) {
        return new CircuitBreakerBuilderImpl<>(serviceType, strategy);
    }

    static <T> CircuitBreakerBuilder<T> newBuilder(Class<T> type) {
        return newBuilder(ServiceType.serviceType(type));
    }

    static <T> CircuitBreakerBuilder<T> newBuilder(Class<T> type, Strategy strategy) {
        return newBuilder(ServiceType.serviceType(type), strategy);
    }

    static <T> CircuitBreaker circuitBreaker(T object) {
        Object handler = object;
        if (Proxy.isProxyClass(handler.getClass())) {
            handler = Proxy.getInvocationHandler(handler);
        }
        if (handler instanceof CircuitBreaker) {
            return (CircuitBreaker) handler;
        }
        throw new IllegalArgumentException("The given object is not backed by a circuit breaker");
    }
}
