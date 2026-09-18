package me.wyne.infpoints.api.config;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

/**
 * Display configuration of a point: how its balance is named and formatted when shown to players.
 *
 * @param name          singular display name
 * @param pluralName    plural display name
 * @param symbol        currency symbol
 * @param color         display color, as used by the plugin's message formatting
 * @param decimalFormat format used to render balances, e.g. via {@link me.wyne.infpoints.api.Point#getFormat}
 */
public record VisualConfig(@NotNull String name, @NotNull String pluralName, @NotNull String symbol, @NotNull String color,
                           @NotNull DecimalFormat decimalFormat) {

    /**
     * Returns a copy of the configured format, since {@link DecimalFormat} is not safe to share between threads.
     */
    @Override
    public @NotNull DecimalFormat decimalFormat() {
        return (DecimalFormat) decimalFormat.clone();
    }

}
