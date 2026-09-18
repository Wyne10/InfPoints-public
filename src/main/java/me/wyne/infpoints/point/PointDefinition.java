package me.wyne.infpoints.point;

import me.wyne.wutils.config.configurables.attribute.GenericFactory;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.api.PointStorageType;
import me.wyne.infpoints.api.config.CommandConfig;
import me.wyne.infpoints.api.config.PointConfig;
import me.wyne.infpoints.api.config.VisualConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public record PointDefinition(@NotNull PointConfig config, @NotNull VisualConfig visualConfig, @NotNull CommandConfig commandConfig,
                              long defaultUnits) {

    public static final int DEFAULT_DECIMALS = 2;

    public @NotNull String key() {
        return config.key();
    }

    public @NotNull PointStorageType type() {
        return config.type();
    }

    public int decimals() {
        return config.decimals();
    }

    public long toUnits(double amount) {
        return Amounts.toUnits(amount, decimals());
    }

    public double toAmount(long units) {
        return Amounts.toAmount(units, decimals());
    }

    public static final class Factory implements GenericFactory<PointDefinition> {

        private static final Pattern KEY = Pattern.compile("[A-Za-z0-9-]{1,64}");

        @Override
        public @NotNull PointDefinition create(@NotNull String key, @NotNull ConfigurationSection config) {
            if (!KEY.matcher(key).matches())
                throw new IllegalArgumentException("Point keys may only contain up to 64 letters, digits and '-'");
            String typeName = config.getString("type");
            if (typeName == null)
                throw new IllegalArgumentException("Missing 'type'");
            if (typeName.equalsIgnoreCase("JSON"))
                throw new IllegalArgumentException("JSON storage was removed in InfPoints 3.0, set 'type: SQL' to import the balances from its data file");
            PointStorageType type;
            try {
                type = PointStorageType.valueOf(typeName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown type '" + typeName + "'");
            }

            boolean vanilla = type == PointStorageType.LEVEL || type == PointStorageType.EXP;
            if (vanilla && config.getInt("decimals", 0) != 0)
                InfPoints.logger().warn("Point '{}' ignores 'decimals', vanilla experience is always whole", key);
            int decimals = vanilla ? 0 : config.getInt("decimals", DEFAULT_DECIMALS);
            if (decimals < 0 || decimals > Amounts.MAX_DECIMALS)
                throw new IllegalArgumentException("'decimals' must be between 0 and " + Amounts.MAX_DECIMALS);

            double defaultBalance = vanilla ? 0 : config.getDouble("defaultBalance", 0);
            long defaultUnits;
            try {
                defaultUnits = Amounts.toUnits(defaultBalance, decimals);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("'defaultBalance' is not a valid amount");
            }
            if (defaultUnits < 0)
                throw new IllegalArgumentException("'defaultBalance' can't be negative");

            String decimalFormat = config.getString("decimalFormat", decimals == 0 ? "#" : "#." + "#".repeat(decimals));
            DecimalFormat format;
            try {
                format = new DecimalFormat(decimalFormat);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid 'decimalFormat' '" + decimalFormat + "'");
            }

            return new PointDefinition(
                    new PointConfig(key, type, Amounts.toAmount(defaultUnits, decimals), decimals),
                    new VisualConfig(
                            config.getString("name", ""),
                            config.getString("namePlural", ""),
                            config.getString("symbol", ""),
                            config.getString("color", ""),
                            format
                    ),
                    new CommandConfig(
                            config.getString("payCommand"),
                            config.getStringList("payAliases"),
                            config.getString("balanceCommand"),
                            config.getStringList("balanceAliases")
                    ),
                    defaultUnits
            );
        }

    }

}
