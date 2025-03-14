package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import org.bigcraft.infpoints.core.PointManager;

public class CoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PointManager.class);
    }
}
