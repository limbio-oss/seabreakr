package io.limb.seabreakr;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixObservableCommand;
import io.limb.seabreakr.spi.EventListener;
import reactor.core.publisher.Mono;
import rx.Observable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class HystrixCircuitBreaker<T>
        extends AbstractCircuitBreaker<T>
        implements InvocationHandler {

    private final HystrixCommandGroupKey groupKey;
    private final Metrics metrics;


    protected HystrixCircuitBreaker(Class<T> type, T backend, T failover,
                                    EventListener listener, int numOfBufferedEvents) {

        // TODO
        super(type, backend, failover, listener, numOfBufferedEvents, ContextImpl::new);

        this.groupKey = HystrixCommandGroupKey.Factory.asKey(type.getName());

        //HystrixCommandMetrics hystrixMetrics = new ReflectiveCommand<>(null, null, groupKey).getMetrics();
        this.metrics = new HystricsMetrics(/*hystrixMetrics*/ null);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        ReflectiveCommand<?> command = new ReflectiveCommand<>(method, backend, args, groupKey);

        if (Mono.class.isAssignableFrom(method.getReturnType())) {
            return Mono.create(s -> command.observe().forEach(s::success, s::error));
        }

        return command.execute();
    }

    private static class ReflectiveCommand<V>
            extends HystrixCommand<V> {

        private final Method method;
        private final Object proxy;
        private final Object[] args;

        ReflectiveCommand(Method method, Object proxy, Object[] args, HystrixCommandGroupKey group) {
            super(group);
            this.method = method;
            this.proxy = proxy;
            this.args = args;
        }

        @Override
        protected V run() throws Exception {
            return (V) method.invoke(proxy, args);
        }

        @Override
        protected V getFallback() {
            return super.getFallback();
        }
    }

    private static class ReflectiveObservableCommand<V>
            extends HystrixObservableCommand<V> {

        protected ReflectiveObservableCommand(HystrixCommandGroupKey group) {
            super(group);
        }

        @Override
        protected Observable<V> construct() {
            return null;
        }

        @Override
        protected Observable<V> resumeWithFallback() {
            return super.resumeWithFallback();
        }
    }

    private static class HystricsMetrics implements Metrics {

        private final HystrixCommandMetrics hystrixMetrics;

        public HystricsMetrics(HystrixCommandMetrics hystrixMetrics) {
            this.hystrixMetrics = hystrixMetrics;
        }

        @Override
        public float getFailureRate() {
            return hystrixMetrics.getHealthCounts().getErrorPercentage();
        }

        @Override
        public long getNumberOfFailedEvents() {
            return hystrixMetrics.getHealthCounts().getErrorCount();
        }

        @Override
        public long getNumberOfBufferedEvents() {
            return hystrixMetrics.getHealthCounts().getTotalRequests();
        }
    }
}
