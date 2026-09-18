package org.bigcraft.infpoints.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerOfflineException extends StorageException {

    public PlayerOfflineException(UUID player) {
        super("Player " + player + " is offline");
    }

    static @NotNull Player requireOnline(@Nullable UUID player) throws PlayerOfflineException {
        Player online = player == null ? null : Bukkit.getPlayer(player);
        if (online == null)
            throw new PlayerOfflineException(player);
        return online;
    }

}
