package io.limb.seabreakr.spi;

public interface ContextFactory {

    Context newContext(int numOfBufferedEvents, EventPublisher eventPublisher);

}
