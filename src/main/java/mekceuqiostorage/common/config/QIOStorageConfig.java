package mekceuqiostorage.common.config;

/**
 * Stable defaults shared by every resource integration.
 *
 * <p>The values are intentionally integer based. QIO stores whole resource units, while an
 * integration is responsible for converting its native representation to this unit before it
 * reaches the transfer layer.</p>
 */
public final class QIOStorageConfig {

    public static final int CODEC_VERSION = 1;
    public static final long DEFAULT_STORAGE_UNITS_PER_UNIT = 1L;
    public static final boolean DEFAULT_CONTRIBUTES_TO_MIXED = true;
    public static final int TYPE_CAPACITY_FROM_DEFINITION = 0;
    public static final int SINGLE_RESOURCE_TYPE_CAPACITY = 1;
    public static final int BLOODMAGIC_WILL_TYPE_CAPACITY = 5;
    /** Will transfers use whole native Will per QIO unit; fractional native remainders stay put. */
    public static final int BLOODMAGIC_WILL_DECIMAL_PLACES = 0;

    /** Astral Sorcery's network amount is converted to the altar's internal 200-point unit. */
    public static final long ASTRALSORCERY_NETWORK_UNITS_PER_QIO_UNIT = 200L;
    public static final long ASTRALSORCERY_MAX_INPUT_PER_TICK = 100L;

    private QIOStorageConfig() {
    }
}
