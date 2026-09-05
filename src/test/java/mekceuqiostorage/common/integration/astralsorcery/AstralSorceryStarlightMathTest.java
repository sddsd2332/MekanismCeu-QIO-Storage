package mekceuqiostorage.common.integration.astralsorcery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AstralSorceryStarlightMathTest {

    @Test
    void convertsNetworkUnitsToTheAltarStorageUnit() {
        assertEquals(200, AstralSorceryStarlightMath.toQIOUnits(1.0));
        assertEquals(250, AstralSorceryStarlightMath.toQIOUnits(1.25));
        assertEquals(0, AstralSorceryStarlightMath.toQIOUnits(0.004));
        assertEquals(0, AstralSorceryStarlightMath.toQIOUnits(Double.NaN));
        assertEquals(0, AstralSorceryStarlightMath.toQIOUnits(-1));
    }

    @Test
    void convertsQIOUnitsBackToNetworkAmount() {
        assertEquals(1.0, AstralSorceryStarlightMath.toNetworkAmount(200), 0.000001);
        assertEquals(0.5, AstralSorceryStarlightMath.toNetworkAmount(100), 0.000001);
        assertEquals(0, AstralSorceryStarlightMath.toNetworkAmount(0), 0.000001);
    }
}
