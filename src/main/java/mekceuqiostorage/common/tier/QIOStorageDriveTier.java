package mekceuqiostorage.common.tier;

import mekanism.common.content.qio.QIOAmount;
import mekanism.common.content.qio.QIODriveDefinition;
import mekanism.common.content.qio.QIOStorageUnits;
import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.config.QIOStorageConfig;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Objects;

/** Stable addon view of Mekanism's four built-in physical drive definitions. */
public enum QIOStorageDriveTier {
    BASE("base", QIODriveDefinition.BASE),
    HYPER_DENSE("hyper_dense", QIODriveDefinition.HYPER_DENSE),
    TIME_DILATING("time_dilating", QIODriveDefinition.TIME_DILATING),
    SUPERMASSIVE("supermassive", QIODriveDefinition.SUPERMASSIVE);

    private final String path;
    private final QIODriveDefinition definition;
    private final QIODriveDefinition singleTypeDefinition;
    private final QIODriveDefinition willDefinition;

    QIOStorageDriveTier(String path, QIODriveDefinition definition) {
        this.path = path;
        this.definition = definition;
        singleTypeDefinition = QIODriveDefinition.builder(
                    QIOStorage.id(path + "_single_type"))
              .baseTier(definition.getBaseTier())
              .maxCount(definition.getMaxCount())
              .maxTypes(1)
              .register();
        willDefinition = QIODriveDefinition.builder(QIOStorage.id(path + "_will"))
              .baseTier(definition.getBaseTier())
              .maxCount(definition.getMaxCount())
              .maxTypes(QIOStorageConfig.BLOODMAGIC_WILL_TYPE_CAPACITY)
              .register();
    }

    @Nonnull
    public String getPath() {
        return path;
    }

    @Nonnull
    public QIODriveDefinition getDefinition() {
        return definition;
    }

    /** Returns the physical definition whose persisted type limit matches this resource. */
    @Nonnull
    public QIODriveDefinition getDefinition(@Nonnull QIOStorageResourceSpec<?> spec) {
        Objects.requireNonNull(spec, "resource specification");
        int typeCapacity = spec.getTypeCapacity(definition);
        if (typeCapacity == definition.getMaxTypes()) {
            return definition;
        }
        if (typeCapacity == 1) {
            return singleTypeDefinition;
        }
        if (typeCapacity == QIOStorageConfig.BLOODMAGIC_WILL_TYPE_CAPACITY) {
            return willDefinition;
        }
        throw new IllegalArgumentException("Unsupported fixed QIO type capacity " + typeCapacity +
              " for " + spec.getCodecId());
    }

    public long getItemEquivalentCapacity() {
        return definition.getMaxCount();
    }

    public int getTypeCapacity() {
        return definition.getMaxTypes();
    }

    public int getTypeCapacity(@Nonnull QIOStorageResourceSpec<?> spec) {
        return Objects.requireNonNull(spec, "resource specification").getTypeCapacity(definition);
    }

    @Nonnull
    public QIOAmount getExactStorageCapacity() {
        return QIOAmount.of(definition.getMaxCount()).multiply(QIOStorageUnits.UNITS_PER_ITEM);
    }

    /** Exact number of whole resource units available for the supplied codec conversion. */
    @Nonnull
    public BigInteger getResourceCapacity(@Nonnull QIOStorageResourceSpec<?> spec) {
        Objects.requireNonNull(spec, "resource specification");
        return getExactStorageCapacity().toBigInteger().divide(
              BigInteger.valueOf(spec.getStorageUnitsPerUnit()));
    }

    @Nullable
    public static QIOStorageDriveTier fromDefinition(@Nullable QIODriveDefinition definition) {
        for (QIOStorageDriveTier tier : values()) {
            if (tier.definition == definition || tier.singleTypeDefinition == definition ||
                  tier.willDefinition == definition) {
                return tier;
            }
        }
        return null;
    }
}
