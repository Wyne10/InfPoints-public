package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.bigcraft.infpoints.core.factory.JsonPointFactory;
import org.bigcraft.infpoints.core.factory.MemoryPointFactory;
import org.bigcraft.infpoints.core.factory.PointFactory;

public class PointTypeModule extends AbstractModule {

    @Override
    protected void configure() {
        MapBinder<String, PointFactory> mapBinder = MapBinder.newMapBinder(binder(), String.class, PointFactory.class);
        mapBinder.addBinding("MEMORY").to(MemoryPointFactory.class);
        mapBinder.addBinding("JSON").to(JsonPointFactory.class);
    }

}
