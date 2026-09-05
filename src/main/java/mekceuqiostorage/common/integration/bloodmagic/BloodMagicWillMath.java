package mekceuqiostorage.common.integration.bloodmagic;

import mekceuqiostorage.common.config.QIOStorageConfig;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;

import java.math.BigDecimal;

/** QIO stores whole Will; smaller native remainders stay in the source. */
public final class BloodMagicWillMath {

    private static final BigDecimal MAXIMUM = BigDecimal.valueOf(Long.MAX_VALUE);

    private BloodMagicWillMath() {
    }

    public static long toQIOUnits(double will) {
        if (!QIOStorageTransferMath.isFinitePositive(will)) {
            return 0;
        }
        // BigDecimal keeps the floor conversion deterministic for native floating-point values.
        BigDecimal units = BigDecimal.valueOf(will).movePointRight(QIOStorageConfig.BLOODMAGIC_WILL_DECIMAL_PLACES);
        return units.compareTo(MAXIMUM) >= 0 ? Long.MAX_VALUE : units.longValue();
    }

    public static double toWill(long units) {
        return units <= 0 ? 0 : BigDecimal.valueOf(units,
              QIOStorageConfig.BLOODMAGIC_WILL_DECIMAL_PLACES).doubleValue();
    }
}
