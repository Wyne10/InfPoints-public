package org.bigcraft.infpoints.api;

import org.jetbrains.annotations.Nullable;

public interface PointProvider {

    @Nullable PointType getPoint(String key);
    @Nullable PointConfig getPointConfig(String key);

}
