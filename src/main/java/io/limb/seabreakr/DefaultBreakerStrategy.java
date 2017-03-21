package io.limb.seabreakr;

import io.limb.seabreakr.spi.Context;
import io.limb.seabreakr.spi.EventListener;
import io.limb.seabreakr.spi.Strategy;

import java.util.Random;

public class DefaultBreakerStrategy
        implements Strategy {

    public static final float DEFAULT_FAILURE_THRESHOLD = 20.f;

    public static final Strategy INSTANCE = new DefaultBreakerStrategy(DEFAULT_FAILURE_THRESHOLD);

    private final Random random = new Random();
    private final float failureThreshold;

    private DefaultBreakerStrategy(float failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    @Override
    public boolean isCallAllowed(Context context) {
        return context.isCallAllowed() || isAvailabilityCheck();
    }

    @Override
    public void onFailure(Context context, EventListener listener) {
        MetricsRecorder metricsRecorder = context.getMetricsRecorder();
        metricsRecorder.recordFailure();
        verifyFailureThreshold(context, metricsRecorder);
    }

    @Override
    public void onSuccess(Context context, EventListener listener) {
        MetricsRecorder metricsRecorder = context.getMetricsRecorder();
        metricsRecorder.recordSuccess();
        verifyFailureThreshold(context, metricsRecorder);
    }

    private boolean isAvailabilityCheck() {
        return random.nextInt(100) <= 20;
    }

    private void verifyFailureThreshold(Context context, Metrics metrics) {
        if (metrics.getFailureRate() > failureThreshold) {
            if (context.getState() == State.Closed) {
                if (context.open()) {
                    context.getEventPublisher().fireOpenState();
                }
            }
        } else {
            /*if (context.getState() != BreakerState.Closed) {
                if (context.close()) {
                    context.getEventPublisher().fireClosedState();
                }
            }*/
        }
    }
}
