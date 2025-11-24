package org.bigcraft.infpoints.api;

import java.util.UUID;

public interface Point extends PointType, PointConfig, PointView {
    String getFormat(UUID player);
}
