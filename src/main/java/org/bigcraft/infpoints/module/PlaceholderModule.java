package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import me.wyne.wutils.log.Log;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.placeholder.PointsPlaceholders;

public class PlaceholderModule extends AbstractModule {
    @Override
    protected void configure() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            bind(PointsPlaceholders.class);
        } catch (ClassNotFoundException e) {
            InfPoints.getInstance().getLog().warn("PlaceholderAPI not found, placeholders are not registered");
        }
    }
}
