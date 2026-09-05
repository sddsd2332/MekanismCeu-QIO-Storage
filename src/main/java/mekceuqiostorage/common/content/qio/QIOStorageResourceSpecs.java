package mekceuqiostorage.common.content.qio;

import mekceuqiostorage.common.config.QIOStorageConfig;
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Scalar;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import mekceuqiostorage.common.integration.QIOStorageHooks;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The canonical, centrally maintained list of resources supplied by this addon. */
public final class QIOStorageResourceSpecs {

    public static final QIOStorageResourceSpec<Essentia> THAUMCRAFT_ESSENTIA =
          new QIOStorageResourceSpec<>(QIOStorageHooks.THAUMCRAFT, QIOStorageCodecs.THAUMCRAFT_ESSENTIA,
                "thaumcraft_essentia", "qio.mekceuqiostorage.drive_specialization.thaumcraft_essentia");
    public static final QIOStorageResourceSpec<Scalar> BOTANIA_MANA =
          new QIOStorageResourceSpec<>(QIOStorageHooks.BOTANIA, QIOStorageCodecs.BOTANIA_MANA,
                "botania_mana", "qio.mekceuqiostorage.drive_specialization.botania_mana",
                QIOStorageConfig.SINGLE_RESOURCE_TYPE_CAPACITY);
    public static final QIOStorageResourceSpec<SoulNetworkLP> BLOODMAGIC_LP =
          new QIOStorageResourceSpec<>(QIOStorageHooks.BLOODMAGIC, QIOStorageCodecs.BLOODMAGIC_LP,
                "bloodmagic_lp", "qio.mekceuqiostorage.drive_specialization.bloodmagic_lp",
                QIOStorageConfig.SINGLE_RESOURCE_TYPE_CAPACITY);
    public static final QIOStorageResourceSpec<DemonWill> BLOODMAGIC_WILL =
          new QIOStorageResourceSpec<>(QIOStorageHooks.BLOODMAGIC, QIOStorageCodecs.BLOODMAGIC_WILL,
                "bloodmagic_will", "qio.mekceuqiostorage.drive_specialization.bloodmagic_will",
                QIOStorageConfig.BLOODMAGIC_WILL_TYPE_CAPACITY);
    public static final QIOStorageResourceSpec<Scalar> ASTRALSORCERY_STARLIGHT =
          new QIOStorageResourceSpec<>(QIOStorageHooks.ASTRALSORCERY, QIOStorageCodecs.ASTRALSORCERY_STARLIGHT,
                "astralsorcery_starlight", "qio.mekceuqiostorage.drive_specialization.astralsorcery_starlight",
                QIOStorageConfig.SINGLE_RESOURCE_TYPE_CAPACITY);
    public static final QIOStorageResourceSpec<Scalar> EMBERS_EMBER =
          new QIOStorageResourceSpec<>(QIOStorageHooks.EMBERS, QIOStorageCodecs.EMBERS_EMBER,
                "embers_ember", "qio.mekceuqiostorage.drive_specialization.embers_ember",
                QIOStorageConfig.SINGLE_RESOURCE_TYPE_CAPACITY);
    public static final QIOStorageResourceSpec<Scalar> NATURESAURA_AURA =
          new QIOStorageResourceSpec<>(QIOStorageHooks.NATURESAURA, QIOStorageCodecs.NATURESAURA_AURA,
                "naturesaura_aura", "qio.mekceuqiostorage.drive_specialization.naturesaura_aura",
                QIOStorageConfig.SINGLE_RESOURCE_TYPE_CAPACITY);
    public static final QIOStorageResourceSpec<Scalar> PNEUMATICCRAFT_AIR =
          new QIOStorageResourceSpec<>(QIOStorageHooks.PNEUMATICCRAFT, QIOStorageCodecs.PNEUMATICCRAFT_AIR,
                "pneumaticcraft_air", "qio.mekceuqiostorage.drive_specialization.pneumaticcraft_air",
                QIOStorageConfig.SINGLE_RESOURCE_TYPE_CAPACITY);

    private static final List<QIOStorageResourceSpec<?>> ALL = Collections.unmodifiableList(Arrays.asList(
          THAUMCRAFT_ESSENTIA, BOTANIA_MANA, BLOODMAGIC_LP, BLOODMAGIC_WILL, ASTRALSORCERY_STARLIGHT,
          EMBERS_EMBER, NATURESAURA_AURA, PNEUMATICCRAFT_AIR));

    private QIOStorageResourceSpecs() {
    }

    @Nonnull
    public static List<QIOStorageResourceSpec<?>> all() {
        return ALL;
    }
}
