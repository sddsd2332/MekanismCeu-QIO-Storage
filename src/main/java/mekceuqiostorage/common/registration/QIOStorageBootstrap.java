package mekceuqiostorage.common.registration;

import mekanism.api.qio.resource.QIOResourceCodec;
import mekanism.api.qio.resource.QIOResourceCodecRegistry;
import mekanism.api.qio.resource.QIOResourceFamilyMatcher;
import mekanism.common.content.qio.QIODriveSpecialization;
import mekanism.common.content.qio.QIODriveSpecializationRegistry;
import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.integration.QIOStorageHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Owns the common QIO registration order. This class has no references to optional-mod APIs, so
 * it is safe to load on either physical side with any subset of integrations installed.
 */
public final class QIOStorageBootstrap {

    private static final Map<net.minecraft.util.ResourceLocation, QIODriveSpecialization> SPECIALIZATIONS =
          new LinkedHashMap<>();
    private static boolean codecsRegistered;
    private static boolean specializationsRegistered;

    private QIOStorageBootstrap() {
    }

    /** Registers every addon codec exactly once. Codecs remain useful for persisted data even if
     * their optional provider is not installed in the current instance. */
    public static synchronized void registerCodecs() {
        if (codecsRegistered) {
            return;
        }
        for (QIOStorageResourceSpec<?> spec : QIOStorageResourceSpecs.all()) {
            QIOResourceCodec<?> codec = spec.getCodec();
            QIOResourceCodec<?> existing = QIOResourceCodecRegistry.INSTANCE.get(codec.getCodecId());
            if (existing == null) {
                registerCodecUnchecked(codec);
            } else if (existing != codec) {
                throw new IllegalStateException("QIO codec id is already owned by another codec: " +
                      codec.getCodecId());
            }
        }
        codecsRegistered = true;
    }

    /**
     * Registers only specializations whose Forge mod id is present. The check is repeated when
     * called from the item registry event so the method is robust to Forge lifecycle ordering.
     */
    public static synchronized void registerAvailableSpecializations() {
        registerCodecs();
        if (specializationsRegistered && allLoadedSpecializationsPresent()) {
            return;
        }
        for (QIOStorageResourceSpec<?> spec : QIOStorageResourceSpecs.all()) {
            if (!isModLoaded(spec.getModId()) || SPECIALIZATIONS.containsKey(spec.getCodecId())) {
                continue;
            }
            QIODriveSpecialization specialization = registerSpecialization(spec);
            SPECIALIZATIONS.put(spec.getCodecId(), specialization);
            QIOStorage.LOGGER.info("Enabled QIO resource integration foundation for {} ({})",
                  spec.getModId(), spec.getCodecId());
        }
        specializationsRegistered = true;
    }

    public static synchronized void initialize() {
        registerAvailableSpecializations();
    }

    public static synchronized boolean isResourceEnabled(@Nonnull QIOStorageResourceSpec<?> spec) {
        Objects.requireNonNull(spec, "resource specification");
        registerAvailableSpecializations();
        return SPECIALIZATIONS.containsKey(spec.getCodecId());
    }

    @Nullable
    public static synchronized QIODriveSpecialization getSpecialization(
          @Nonnull QIOStorageResourceSpec<?> spec) {
        Objects.requireNonNull(spec, "resource specification");
        registerAvailableSpecializations();
        return SPECIALIZATIONS.get(spec.getCodecId());
    }

    @Nonnull
    public static synchronized List<QIOStorageResourceSpec<?>> getEnabledResources() {
        registerAvailableSpecializations();
        List<QIOStorageResourceSpec<?>> enabled = new ArrayList<>();
        for (QIOStorageResourceSpec<?> spec : QIOStorageResourceSpecs.all()) {
            if (SPECIALIZATIONS.containsKey(spec.getCodecId())) {
                enabled.add(spec);
            }
        }
        return Collections.unmodifiableList(enabled);
    }

    @Nonnull
    public static synchronized Map<net.minecraft.util.ResourceLocation, QIODriveSpecialization>
    getSpecializations() {
        registerAvailableSpecializations();
        return Collections.unmodifiableMap(new LinkedHashMap<>(SPECIALIZATIONS));
    }

    private static boolean allLoadedSpecializationsPresent() {
        for (QIOStorageResourceSpec<?> spec : QIOStorageResourceSpecs.all()) {
            if (isModLoaded(spec.getModId()) && !SPECIALIZATIONS.containsKey(spec.getCodecId())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isModLoaded(String modId) {
        return QIOStorageHooks.isLoaded(modId);
    }

    static QIODriveSpecialization registerSpecialization(QIOStorageResourceSpec<?> spec) {
        Objects.requireNonNull(spec, "resource specification");
        if (QIOResourceCodecRegistry.INSTANCE.get(spec.getCodecId()) != spec.getCodec()) {
            throw new IllegalStateException("QIO codec must be registered before specialization " +
                  spec.getSpecializationId());
        }
        if (QIODriveSpecializationRegistry.INSTANCE.get(spec.getSpecializationId()) != null) {
            throw new IllegalStateException("QIO specialization id is already registered: " +
                  spec.getSpecializationId());
        }
        return QIODriveSpecialization.builder(spec.getSpecializationId())
              .matcher(QIOResourceFamilyMatcher.codec(spec.getCodecId()))
              .capacityFromDefinition()
              .contributesToMixed(spec.contributesToMixed())
              .translationKey(spec.getTranslationKey())
              .register();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerCodecUnchecked(QIOResourceCodec<?> codec) {
        QIOResourceCodecRegistry.INSTANCE.register((QIOResourceCodec) codec);
    }
}
