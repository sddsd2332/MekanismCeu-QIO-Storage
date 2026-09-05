package mekceuqiostorage.common.content.qio;

import mekanism.api.qio.resource.QIOResourceCodec;
import mekanism.api.qio.resource.QIOResourceFamily;
import mekanism.common.content.qio.QIODriveDefinition;
import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.config.QIOStorageConfig;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Objects;

/** Immutable registration metadata for one addon-owned QIO resource family. */
public final class QIOStorageResourceSpec<T> {

    private final String modId;
    private final QIOResourceCodec<T> codec;
    private final ResourceLocation specializationId;
    private final String itemName;
    private final String translationKey;
    private final boolean contributesToMixed;
    private final int fixedTypeCapacity;

    public QIOStorageResourceSpec(@Nonnull String modId, @Nonnull QIOResourceCodec<T> codec,
          @Nonnull String itemName, @Nonnull String translationKey) {
        this(modId, codec, itemName, translationKey, QIOStorageConfig.DEFAULT_CONTRIBUTES_TO_MIXED,
              QIOStorageConfig.TYPE_CAPACITY_FROM_DEFINITION);
    }

    public QIOStorageResourceSpec(@Nonnull String modId, @Nonnull QIOResourceCodec<T> codec,
          @Nonnull String itemName, @Nonnull String translationKey, int fixedTypeCapacity) {
        this(modId, codec, itemName, translationKey, QIOStorageConfig.DEFAULT_CONTRIBUTES_TO_MIXED,
              fixedTypeCapacity);
    }

    public QIOStorageResourceSpec(@Nonnull String modId, @Nonnull QIOResourceCodec<T> codec,
          @Nonnull String itemName, @Nonnull String translationKey, boolean contributesToMixed) {
        this(modId, codec, itemName, translationKey, contributesToMixed,
              QIOStorageConfig.TYPE_CAPACITY_FROM_DEFINITION);
    }

    public QIOStorageResourceSpec(@Nonnull String modId, @Nonnull QIOResourceCodec<T> codec,
          @Nonnull String itemName, @Nonnull String translationKey, boolean contributesToMixed,
          int fixedTypeCapacity) {
        this.modId = requireModId(modId);
        this.codec = Objects.requireNonNull(codec, "QIO resource codec");
        ResourceLocation codecId = Objects.requireNonNull(codec.getCodecId(), "QIO codec id");
        if (!QIOStorage.MODID.equals(codecId.getNamespace())) {
            throw new IllegalArgumentException("Addon resource codec must use namespace " +
                  QIOStorage.MODID + ": " + codecId);
        }
        QIOResourceFamily.requireValid(codec.getFamily());
        if (codec.getCodecVersion() <= 0) {
            throw new IllegalArgumentException("QIO codec version must be positive: " + codecId);
        }
        if (codec.getStorageUnitsPerUnit() <= 0) {
            throw new IllegalArgumentException("QIO storage units must be positive: " + codecId);
        }
        this.itemName = requirePath(itemName);
        this.translationKey = requireTranslationKey(translationKey);
        this.contributesToMixed = contributesToMixed;
        if (fixedTypeCapacity < QIOStorageConfig.TYPE_CAPACITY_FROM_DEFINITION) {
            throw new IllegalArgumentException("QIO fixed type capacity cannot be negative: " +
                  fixedTypeCapacity);
        }
        this.fixedTypeCapacity = fixedTypeCapacity;
        specializationId = QIOStorage.id(itemName);
    }

    @Nonnull
    public String getModId() {
        return modId;
    }

    @Nonnull
    public QIOResourceCodec<T> getCodec() {
        return codec;
    }

    @Nonnull
    public ResourceLocation getCodecId() {
        return codec.getCodecId();
    }

    @Nonnull
    public String getFamily() {
        return codec.getFamily();
    }

    @Nonnull
    public ResourceLocation getSpecializationId() {
        return specializationId;
    }

    @Nonnull
    public String getItemName() {
        return itemName;
    }

    @Nonnull
    public String getTranslationKey() {
        return translationKey;
    }

    public long getStorageUnitsPerUnit() {
        return codec.getStorageUnitsPerUnit();
    }

    public boolean contributesToMixed() {
        return contributesToMixed;
    }

    public int getTypeCapacity(@Nonnull QIODriveDefinition definition) {
        Objects.requireNonNull(definition, "QIO drive definition");
        return fixedTypeCapacity == QIOStorageConfig.TYPE_CAPACITY_FROM_DEFINITION ?
              definition.getMaxTypes() : fixedTypeCapacity;
    }

    public boolean hasFixedTypeCapacity() {
        return fixedTypeCapacity != QIOStorageConfig.TYPE_CAPACITY_FROM_DEFINITION;
    }

    @Override
    public String toString() {
        return getCodecId().toString();
    }

    private static String requireModId(String value) {
        Objects.requireNonNull(value, "optional mod id");
        if (value.isEmpty() || !value.equals(value.toLowerCase(java.util.Locale.ROOT)) ||
              !value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid optional mod id: " + value);
        }
        return value;
    }

    private static String requirePath(String value) {
        Objects.requireNonNull(value, "itemName");
        if (value.isEmpty() || !value.equals(value.toLowerCase(java.util.Locale.ROOT)) ||
              !value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid QIO resource path: " + value);
        }
        return value;
    }

    private static String requireTranslationKey(String value) {
        Objects.requireNonNull(value, "translationKey");
        if (value.isEmpty() || value.length() > 256) {
            throw new IllegalArgumentException("Invalid QIO resource translation key");
        }
        return value;
    }
}
