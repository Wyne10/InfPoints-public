package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import me.wyne.wutils.log.Log;

public class CommandModule extends AbstractModule {
    @Override
    protected void configure() {
        try {
            Class.forName("dev.jorel.commandapi.CommandAPI");
        } catch (ClassNotFoundException e) {
            Log.global.warn("CommandAPI not found, commands are not registered");
        }
    }
}
