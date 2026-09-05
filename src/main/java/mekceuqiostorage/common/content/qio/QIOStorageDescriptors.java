package mekceuqiostorage.common.content.qio;

import mekanism.api.qio.resource.QIOResourceCodec;
import mekanism.api.qio.resource.QIOResourceCodecRegistry;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;

import javax.annotation.Nonnull;
import java.util.UUID;

/** Canonical descriptor factories used by transfer and client selection integrations. */
public final class QIOStorageDescriptors {

    private QIOStorageDescriptors() {
    }

    @Nonnull
    public static QIOResourceDescriptor essentia(@Nonnull String aspectTag) {
        return create(QIOStorageCodecs.THAUMCRAFT_ESSENTIA, Essentia.of(aspectTag));
    }

    @Nonnull
    public static QIOResourceDescriptor mana() {
        return create(QIOStorageCodecs.BOTANIA_MANA, QIOStorageResources.MANA);
    }

    @Nonnull
    public static QIOResourceDescriptor lp(@Nonnull UUID ownerId) {
        return create(QIOStorageCodecs.BLOODMAGIC_LP, SoulNetworkLP.of(ownerId));
    }

    @Nonnull
    public static QIOResourceDescriptor will(@Nonnull DemonWill type) {
        return create(QIOStorageCodecs.BLOODMAGIC_WILL, type);
    }

    @Nonnull
    public static QIOResourceDescriptor starlight() {
        return create(QIOStorageCodecs.ASTRALSORCERY_STARLIGHT, QIOStorageResources.STARLIGHT);
    }

    @Nonnull
    public static QIOResourceDescriptor ember() {
        return create(QIOStorageCodecs.EMBERS_EMBER, QIOStorageResources.EMBER);
    }

    @Nonnull
    public static QIOResourceDescriptor aura() {
        return create(QIOStorageCodecs.NATURESAURA_AURA, QIOStorageResources.AURA);
    }

    @Nonnull
    public static QIOResourceDescriptor air() {
        return create(QIOStorageCodecs.PNEUMATICCRAFT_AIR, QIOStorageResources.AIR);
    }

    @Nonnull
    private static <T> QIOResourceDescriptor create(@Nonnull QIOResourceCodec<T> codec,
          @Nonnull T resource) {
        if (QIOResourceCodecRegistry.INSTANCE.get(codec.getCodecId()) != codec) {
            QIOStorageBootstrap.registerCodecs();
        }
        return QIOResourceDescriptor.of(codec, resource);
    }
}
