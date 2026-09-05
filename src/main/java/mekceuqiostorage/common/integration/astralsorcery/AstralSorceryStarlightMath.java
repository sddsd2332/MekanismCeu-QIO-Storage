package mekceuqiostorage.common.integration.astralsorcery;

import mekceuqiostorage.common.config.QIOStorageConfig;

/** Unit conversion and bounds shared by the Astral Sorcery bridge. */
public final class AstralSorceryStarlightMath {

    private AstralSorceryStarlightMath() {
    }

    public static long toQIOUnits(double networkAmount) {
        if (!Double.isFinite(networkAmount) || networkAmount <= 0) {
            return 0;
        }
        double scaled = networkAmount * QIOStorageConfig.ASTRALSORCERY_NETWORK_UNITS_PER_QIO_UNIT;
        if (!Double.isFinite(scaled) || scaled >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) Math.floor(scaled);
    }

    public static double toNetworkAmount(long qioUnits) {
        if (qioUnits <= 0) {
            return 0;
        }
        return (double) qioUnits / QIOStorageConfig.ASTRALSORCERY_NETWORK_UNITS_PER_QIO_UNIT;
    }
}
