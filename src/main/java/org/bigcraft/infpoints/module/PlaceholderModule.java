package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import me.wyne.wutils.log.Log;

public class PlaceholderModule extends AbstractModule {
    @Override
    protected void configure() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
        } catch (ClassNotFoundException e) {
            Log.global.warn("PlaceholderAPI not found, placeholders are not registered");
        }
    }
}
