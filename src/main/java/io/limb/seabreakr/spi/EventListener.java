package io.limb.seabreakr.spi;

import io.limb.seabreakr.Event;

public interface EventListener {

    void onEvent(Event event);

}
