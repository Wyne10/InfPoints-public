package me.wyne.infpoints.point;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Amounts {

    public static final int MAX_DECIMALS = 8;

    private Amounts() {}

    public static long toUnits(double amount, int decimals) {
        if (!Double.isFinite(amount))
            throw new ArithmeticException("Amount " + amount + " is not a finite number");
        return BigDecimal.valueOf(amount)
                .setScale(decimals, RoundingMode.HALF_UP)
                .unscaledValue()
                .longValueExact();
    }

    public static double toAmount(long units, int decimals) {
        return BigDecimal.valueOf(units, decimals).doubleValue();
    }

    public static BigDecimal toDecimal(long units, int decimals) {
        return BigDecimal.valueOf(units, decimals);
    }

    public static long rescale(long units, int fromDecimals, int toDecimals) {
        return BigDecimal.valueOf(units, fromDecimals)
                .setScale(toDecimals, RoundingMode.HALF_UP)
                .unscaledValue()
                .longValueExact();
    }

}
