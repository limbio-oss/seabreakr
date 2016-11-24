package io.limb.seabreakr;

import reactor.core.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public interface CircuitBreakerBuilder<T> {
    CircuitBreakerBuilder<T> backend(T backend);

    CircuitBreakerBuilder<T> failover(T failover);

    CircuitBreakerBuilder<T> deadline(long timeout, TimeUnit timeUnit);

    CircuitBreakerBuilder<T> listener(BreakerEventListener listener);

    CircuitBreakerBuilder<T> enableCallThrough();

    CircuitBreakerBuilder<T> disableCallThrough();

    CircuitBreakerBuilder<T> scheduler(Scheduler scheduler);

    T build();
}
