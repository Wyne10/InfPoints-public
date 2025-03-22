package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import org.bigcraft.infpoints.listener.PointEventListener;

public class ListenerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PointEventListener.class);
    }
}
