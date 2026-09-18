package org.bigcraft.infpoints.point;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AmountsTest {

    @Test
    void roundsHalfUpToDecimals() {
        assertEquals(1235, Amounts.toUnits(12.345, 2));
        assertEquals(1, Amounts.toUnits(0.005, 2));
        assertEquals(0, Amounts.toUnits(0.004, 2));
        assertEquals(13, Amounts.toUnits(12.5, 0));
        assertEquals(-1235, Amounts.toUnits(-12.345, 2));
    }

    @Test
    void repeatedFractionsStayExact() {
        long units = 0;
        for (int i = 0; i < 10; i++)
            units += Amounts.toUnits(0.1, 2);
        assertEquals(100, units);
        assertEquals(1.0, Amounts.toAmount(units, 2));
    }

    @Test
    void rejectsNonFiniteAndOverflowingAmounts() {
        assertThrows(ArithmeticException.class, () -> Amounts.toUnits(Double.NaN, 2));
        assertThrows(ArithmeticException.class, () -> Amounts.toUnits(Double.POSITIVE_INFINITY, 2));
        assertThrows(ArithmeticException.class, () -> Amounts.toUnits(1e18, 2));
    }

    @Test
    void rescalesBetweenDecimals() {
        assertEquals(12300, Amounts.rescale(123, 0, 2));
        assertEquals(124, Amounts.rescale(12350, 2, 0));
    }

}
