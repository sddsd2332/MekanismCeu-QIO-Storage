package mekceuqiostorage.common.registration;

import mekanism.common.Mekanism;
import mekanism.common.content.qio.QIODriveDefinition;
import mekanism.common.content.qio.QIODriveSpecialization;
import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lazily creates and registers the four physical drives for each enabled resource.
 */
public final class QIOStorageItems {

    private static final Map<ResourceLocation, EnumMap<QIOStorageDriveTier, ItemQIOStorageDrive>> DRIVES =
            new LinkedHashMap<>();
    private static boolean registered;

    private QIOStorageItems() {
    }

    /**
     * Must be called from the Forge item registry event. No item is constructed for an absent
     * optional mod, which keeps both registry contents and class loading side-safe.
     */
    public static synchronized void registerItems(@Nonnull IForgeRegistry<Item> registry) {
        Objects.requireNonNull(registry, "item registry");
        QIOStorageBootstrap.initialize();
        if (registered) {
            return;
        }
        int registeredDrives = 0;
        for (QIOStorageResourceSpec<?> spec : QIOStorageBootstrap.getEnabledResources()) {
            QIODriveSpecialization specialization = QIOStorageBootstrap.getSpecialization(spec);
            if (specialization == null) {
                continue;
            }
            EnumMap<QIOStorageDriveTier, ItemQIOStorageDrive> drives =
                    new EnumMap<>(QIOStorageDriveTier.class);
            for (QIOStorageDriveTier tier : QIOStorageDriveTier.values()) {
                ItemQIOStorageDrive item = new ItemQIOStorageDrive(tier, spec, specialization);
                String path = "qio_drive_" + tier.getPath() + "_" + spec.getItemName();
                item.setTranslationKey(path);
                item.setRegistryName(QIOStorage.id(path));
                item.setCreativeTab(Mekanism.tabMekanism);
                drives.put(tier, item);
                registry.register(item);
                registeredDrives++;
            }
            DRIVES.put(spec.getCodecId(), drives);
        }
        registered = true;
        QIOStorage.LOGGER.info("Registered {} QIO drive items for {} optional resource providers",
                registeredDrives, DRIVES.size());
    }

    @Nonnull
    public static synchronized List<ItemQIOStorageDrive> getRegisteredItems() {
        List<ItemQIOStorageDrive> result = new ArrayList<>();
        for (EnumMap<QIOStorageDriveTier, ItemQIOStorageDrive> drives : DRIVES.values()) {
            result.addAll(drives.values());
        }
        return Collections.unmodifiableList(result);
    }

    @Nullable
    public static synchronized ItemQIOStorageDrive getDrive(@Nonnull ResourceLocation codecId,
                                                            @Nonnull QIODriveDefinition definition) {
        Objects.requireNonNull(codecId, "codec id");
        Objects.requireNonNull(definition, "drive definition");
        EnumMap<QIOStorageDriveTier, ItemQIOStorageDrive> drives = DRIVES.get(codecId);
        if (drives == null) {
            return null;
        }
        QIOStorageDriveTier tier = QIOStorageDriveTier.fromDefinition(definition);
        return tier == null ? null : drives.get(tier);
    }

    public static synchronized boolean isRegistered() {
        return registered;
    }

}
