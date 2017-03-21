package io.limb.seabreakr;

import io.limb.seabreakr.spi.EventListener;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;

public interface CircuitBreakerBuilder<T> {
    CircuitBreakerBuilder<T> backend(T backend);

    CircuitBreakerBuilder<T> failover(T failover);

    CircuitBreakerBuilder<T> deadline(long timeout, TimeUnit timeUnit);

    CircuitBreakerBuilder<T> listener(EventListener listener);

    CircuitBreakerBuilder<T> enableCallThrough();

    CircuitBreakerBuilder<T> disableCallThrough();

    CircuitBreakerBuilder<T> scheduler(Scheduler scheduler);

    T build();
}
