package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.bigcraft.infpoints.api.PointTypes;
import org.bigcraft.infpoints.core.factory.*;

public class PointTypeModule extends AbstractModule {

    @Override
    protected void configure() {
        MapBinder<String, PointFactory> mapBinder = MapBinder.newMapBinder(binder(), String.class, PointFactory.class);
        mapBinder.addBinding(PointTypes.MEMORY.name()).to(MemoryPointFactory.class);
        mapBinder.addBinding(PointTypes.JSON.name()).to(JsonPointFactory.class);
        mapBinder.addBinding(PointTypes.PDC.name()).to(PdcPointFactory.class);
        mapBinder.addBinding(PointTypes.XP.name()).to(XpPointFactory.class);
        mapBinder.addBinding(PointTypes.SQL.name()).to(SqlPointFactory.class);
    }

}
