package org.bigcraft.infpoints.api;

import org.jetbrains.annotations.Nullable;

public interface PointProvider {

    @Nullable Point getPoint(String key);
    @Nullable PointType getPointType(String key);
    @Nullable PointView getPointView(String key);
    @Nullable PointConfig getPointConfig(String key);

}
