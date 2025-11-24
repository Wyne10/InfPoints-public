package org.bigcraft.infpoints.core;

import me.wyne.wutils.json.JsonRegistry;
import org.bigcraft.infpoints.InfPoints;
import org.bukkit.configuration.ConfigurationSection;

public class JsonPoint extends MemoryPoint {

    public JsonPoint(ConfigurationSection config) {
        super(config);
        try {
            JsonRegistry.global.register(this, MemoryPoint.class.getDeclaredField("balance"), "data/" + getConfig().key() + ".json");
        } catch (NoSuchFieldException e) {
            InfPoints.getInstance().getLog().error("An exception occurred trying to register json point", e);
        }
    }

}
