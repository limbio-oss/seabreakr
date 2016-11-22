package io.limb.seabreakr;

import org.junit.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class TestCase {

    @Test
    public void backend_success() {
        Supplier<String> backend = () -> "success";

        ServiceType<Supplier<String>> serviceType = newServiceType();
        Supplier<String> breaker = CircuitBreaker.newBuilder(serviceType).backend(backend).build();

        assertEquals("success", breaker.get());
    }

    @Test(expected = NoSuchFailoverException.class)
    public void backend_failure() {
        Supplier<String> backend = () -> "success";

        ServiceType<Supplier<String>> serviceType = newServiceType();
        Supplier<String> breaker = CircuitBreaker.newBuilder(serviceType).backend(backend).build();

        assertEquals("success", breaker.get());
        CircuitBreaker.circuitBreaker(breaker).open();
        breaker.get();
    }

    @Test
    public void backend_failure_fallback_success() {
        Supplier<String> fallback = () -> "success";
        Supplier<String> backend = () -> "failure";

        ServiceType<Supplier<String>> serviceType = newServiceType();
        Supplier<String> failover = CircuitBreaker.newBuilder(serviceType).backend(fallback).build();
        Supplier<String> breaker = CircuitBreaker.newBuilder(serviceType).backend(backend).failover(failover).build();

        assertEquals("failure", breaker.get());
        CircuitBreaker.circuitBreaker(breaker).open();
        assertEquals("success", breaker.get());
    }

    @Test
    public void backend_deadline_reached_callthrough_enabled_fallback_success() throws Exception {
        Semaphore semaphore = new Semaphore(1);

        ThrowingSupplier<String> fallback = () -> "success";
        ThrowingSupplier<String> backend = () -> {
            semaphore.acquire();
            return "backend";
        };

        ServiceType<ThrowingSupplier<String>> serviceType = newThrowingServiceType();
        ThrowingSupplier<String> failover = CircuitBreaker.newBuilder(serviceType).backend(fallback).build();

        ThrowingSupplier<String> breaker = CircuitBreaker.newBuilder(serviceType).backend(backend).failover(failover)
                .deadline(2, TimeUnit.SECONDS).listener((e) -> semaphore.release()).enableCallThrough().build();

        assertEquals("backend", breaker.get());
        assertEquals("success", breaker.get());
    }

    @Test(expected = NoSuchFailoverException.class)
    public void backend_deadline_reached_callthrough_enabled_no_failover() throws Exception {
        Semaphore semaphore = new Semaphore(1);

        ThrowingSupplier<String> backend = () -> {
            semaphore.acquire();
            return "backend";
        };

        ServiceType<ThrowingSupplier<String>> serviceType = newThrowingServiceType();
        ThrowingSupplier<String> breaker = CircuitBreaker.newBuilder(serviceType).backend(backend)
                .deadline(2, TimeUnit.SECONDS).listener((e) -> semaphore.release()).enableCallThrough().build();

        assertEquals("backend", breaker.get());
        assertEquals("success", breaker.get());
    }

    @Test(expected = CallTimeoutException.class)
    public void backend_deadline_reached_callthrough_disabled() throws Exception {
        Semaphore semaphore = new Semaphore(1);

        ThrowingSupplier<String> backend = () -> {
            semaphore.acquire();
            return "backend";
        };

        ServiceType<ThrowingSupplier<String>> serviceType = newThrowingServiceType();
        ThrowingSupplier<String> breaker = CircuitBreaker.newBuilder(serviceType).backend(backend)
                .deadline(2, TimeUnit.SECONDS).listener((e) -> semaphore.release()).build();

        assertEquals("backend", breaker.get());
        assertEquals("success", breaker.get());
    }

    private ServiceType<Supplier<String>> newServiceType() {
        return new ServiceType<Supplier<String>>() {
        };
    }

    private ServiceType<ThrowingSupplier<String>> newThrowingServiceType() {
        return new ServiceType<ThrowingSupplier<String>>() {
        };
    }
}
