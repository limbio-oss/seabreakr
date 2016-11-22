package io.limb.seabreakr;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

public interface Scheduler<T> {
    void schedule(Predicate<T> predicate, BreakerState status, ScheduledExecutorService scheduler);
}
