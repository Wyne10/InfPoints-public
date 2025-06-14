package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import me.wyne.wutils.log.Log;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.command.InfPointsCommand;

public class CommandModule extends AbstractModule {
    @Override
    protected void configure() {
        try {
            Class.forName("dev.jorel.commandapi.CommandAPI");
            bind(InfPointsCommand.class);
        } catch (ClassNotFoundException e) {
            InfPoints.getInstance().getLog().warn("CommandAPI not found, commands are not registered");
        }
    }
}
