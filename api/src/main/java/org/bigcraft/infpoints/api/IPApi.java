package org.bigcraft.infpoints.api;

public final class IPApi {
    private static PointApi instance;

    public static void setInstance(PointApi instance) {
        IPApi.instance = instance;
    }

    public static PointApi getInstance() {
        return instance;
    }
}
