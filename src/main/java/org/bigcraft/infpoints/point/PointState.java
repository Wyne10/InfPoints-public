package org.bigcraft.infpoints.point;

import org.bigcraft.infpoints.storage.PointStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PointState(@NotNull PointDefinition definition, @NotNull PointStorage storage, @Nullable String unavailableReason) {

    public boolean available() {
        return unavailableReason == null;
    }

    public @NotNull PointState retire(@NotNull String reason) {
        return new PointState(definition, storage, reason);
    }

}
