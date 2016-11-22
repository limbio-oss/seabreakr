package io.limb.seabreakr;

import java.util.Random;

public class DefaultBreakerStrategy
        implements BreakerStrategy {

    public static final float DEFAULT_FAILURE_THRESHOLD = 20.f;

    public static final BreakerStrategy INSTANCE = new DefaultBreakerStrategy(DEFAULT_FAILURE_THRESHOLD);

    private final Random random = new Random();
    private final float failureThreshold;

    private DefaultBreakerStrategy(float failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    @Override
    public boolean isCallAllowed(BreakerContext context) {
        return context.isCallAllowed() || isAvailabilityCheck();
    }

    @Override
    public void onFailure(BreakerContext context, BreakerEventListener listener) {
        BreakerMetricsRecorder metricsRecorder = context.getMetricsRecorder();
        metricsRecorder.recordFailure();
        verifyFailureThreshold(context, metricsRecorder);
    }

    @Override
    public void onSuccess(BreakerContext context, BreakerEventListener listener) {
        BreakerMetricsRecorder metricsRecorder = context.getMetricsRecorder();
        metricsRecorder.recordSuccess();
        verifyFailureThreshold(context, metricsRecorder);
    }

    private boolean isAvailabilityCheck() {
        return random.nextInt(100) <= 20;
    }

    private void verifyFailureThreshold(BreakerContext context, BreakerMetrics metrics) {
        if (metrics.getFailureRate() > failureThreshold) {
            if (context.getState() == BreakerState.Closed) {
                if (context.open()) {
                    context.getEventPublisher().fireOpenState();
                }
            }
        } else {
            if (context.getState() != BreakerState.Closed) {
                if (context.close()) {
                    context.getEventPublisher().fireClosedState();
                }
            }
        }
    }
}
