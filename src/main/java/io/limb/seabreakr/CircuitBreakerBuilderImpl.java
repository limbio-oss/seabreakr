package io.limb.seabreakr;

import io.limb.seabreakr.spi.EventListener;
import io.limb.seabreakr.spi.Strategy;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class CircuitBreakerBuilderImpl<T>
        implements CircuitBreakerBuilder<T> {

    private static final ScheduledExecutorService SCHEDULER = //
            Executors.newSingleThreadScheduledExecutor((r) -> new Thread(r, "SeaBreakr-Opener"));

    static {
        registerShutdownHook();
    }

    private final ServiceType<T> type;
    private final Strategy strategy;

    private final List<EventListener> listeners = new ArrayList<>();

    private T backend;
    private T failover;
    private long timeout = 30;
    private Scheduler scheduler;
    private boolean callThrough = false;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    CircuitBreakerBuilderImpl(ServiceType<T> type) {
        this(type, DefaultBreakerStrategy.INSTANCE);
    }

    CircuitBreakerBuilderImpl(ServiceType<T> type, Strategy strategy) {
        this.type = type;
        this.strategy = strategy;
    }

    @Override
    public CircuitBreakerBuilder<T> backend(T backend) {
        Objects.requireNonNull(backend, "backend cannot be null");
        this.backend = backend;
        return this;
    }

    @Override
    public CircuitBreakerBuilder<T> failover(T failover) {
        this.failover = failover;
        return this;
    }

    @Override
    public CircuitBreakerBuilder<T> deadline(long timeout, TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "timeUnit cannot be null");
        Preconditions.requireGreaterEquals(timeout, 1, "timeout must be greater or equal to 1");
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        return this;
    }

    @Override
    public CircuitBreakerBuilder<T> listener(EventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        return this;
    }

    @Override
    public CircuitBreakerBuilder<T> enableCallThrough() {
        callThrough = true;
        return this;
    }

    @Override
    public CircuitBreakerBuilder<T> disableCallThrough() {
        callThrough = false;
        return this;
    }

    @Override
    public CircuitBreakerBuilder<T> scheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public T build() {
        EventListener listenerAdapter = null;
        if (listeners.size() > 0) {
            EventListener[] array = listeners.toArray(new EventListener[0]);
            listenerAdapter = e -> Arrays.stream(array).forEach(l -> l.onEvent(e));
        }

        long timeout = timeUnit.toNanos(this.timeout);
        Class<? super T> interfaceType = type.getRawType();
        ClassLoader classLoader = type.getRawType().getClassLoader();
        Scheduler scheduler = this.scheduler == null ? Schedulers.elastic() : this.scheduler;

        InvocationHandler invocationHandler = new JavaProxyCircuitBreaker<>(interfaceType, strategy, backend, failover, timeout,
                listenerAdapter, callThrough, scheduler);

        return (T) Proxy.newProxyInstance(classLoader, new Class[]{interfaceType}, invocationHandler);
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(SCHEDULER::shutdown));
    }

}