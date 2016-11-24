package io.limb.seabreakr;

import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class CircuitBreakerTest {

    public static void main(String[] args)
            throws Exception {

        TestFunction original = () -> "foo";
        TestFunction fallback = () -> "bar";

        TestFunction tfFallback = CircuitBreaker.newBuilder(TestFunction.class).backend(fallback) //
                .deadline(1, TimeUnit.SECONDS).build();

        TestFunction tfOriginal = CircuitBreaker.newBuilder(TestFunction.class).backend(original) //
                .failover(tfFallback).build();

        System.out.println(tfOriginal.get()); // foo

        CircuitBreaker.circuitBreaker(tfOriginal).open();
        System.out.println(tfOriginal.get()); // bar

        CircuitBreaker.circuitBreaker(tfFallback).open();
        try {
            System.out.println(tfOriginal.get()); // exception
        } catch (Exception e) {
            e.printStackTrace();
        }

        TestSingle singleOriginal = () -> Mono.just("test-foo");
        TestSingle singleFallback = () -> Mono.just("test-bar");

        TestSingle tsFallback = CircuitBreaker.newBuilder(TestSingle.class).backend(singleFallback) //
                .deadline(1, TimeUnit.SECONDS).build();

        TestSingle tsOriginal = CircuitBreaker.newBuilder(TestSingle.class).backend(singleOriginal) //
                .failover(tsFallback).build();

        tsOriginal.getOne().subscribe(System.out::println); // test-foo

        CircuitBreaker.circuitBreaker(tsOriginal).open();
        tsOriginal.getOne().subscribe(v -> System.out.println(v)); // test-bar
    }

    private interface TestFunction {
        String get();
    }

    private interface TestSingle {
        Mono getOne();
    }

}
