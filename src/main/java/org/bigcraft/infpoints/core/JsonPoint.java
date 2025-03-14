package org.bigcraft.infpoints.core;

import me.wyne.wutils.json.JsonRegistry;
import me.wyne.wutils.log.Log;
import org.bukkit.configuration.ConfigurationSection;

public class JsonPoint extends MemoryPoint {

    public JsonPoint(ConfigurationSection config) {
        super(config);
        try {
            JsonRegistry.global.register(this, MemoryPoint.class.getDeclaredField("balance"), "data/" + config.getName() + ".json");
        } catch (NoSuchFieldException e) {
            Log.global.exception("An exception occurred trying to register json point", e);
        }
    }

}
