package org.bigcraft.infpoints.api;

/**
 * Static service locator for the active {@link PointApi} instance.
 * <p>
 * The InfPoints plugin calls {@link #setInstance(PointApi)} once it has enabled;
 * other plugins should call {@link #getInstance()} only after that has happened
 * (e.g. after a hard or soft dependency on InfPoints has loaded).
 */
public final class IPApi {
    private static PointApi instance;

    /**
     * Sets the active API instance. Intended for use by the InfPoints plugin itself.
     */
    public static void setInstance(PointApi instance) {
        IPApi.instance = instance;
    }

    /**
     * Returns the active API instance, or {@code null} if InfPoints has not
     * finished initializing yet.
     */
    public static PointApi getInstance() {
        return instance;
    }
}
