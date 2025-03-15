package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import org.bigcraft.infpoints.InfPointsApi;
import org.bigcraft.infpoints.api.PointApi;

public class ApiModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PointApi.class).to(InfPointsApi.class);
    }
}
