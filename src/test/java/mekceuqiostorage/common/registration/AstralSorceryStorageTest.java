package mekceuqiostorage.common.registration;

import mekanism.api.Action;
import mekanism.common.content.qio.QIODriveDefinition;
import mekanism.common.content.qio.QIODriveRecord;
import mekanism.common.content.qio.QIODriveSpecialization;
import mekanism.common.content.qio.QIODriveSpecializationRegistry;
import mekanism.common.content.qio.QIOResourceTypeRegistry;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstralSorceryStorageTest {

    private static QIODriveSpecialization specialization;

    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void registerStorage() {
        Bootstrap.register();
        QIOStorageBootstrap.registerCodecs();
        specialization = QIODriveSpecializationRegistry.INSTANCE.get(
              QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT.getSpecializationId());
        if (specialization == null) {
            specialization = QIOStorageBootstrap.registerSpecialization(QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT);
        }
    }

    @AfterEach
    void resetTypes() {
        QIOResourceTypeRegistry.INSTANCE.reset();
    }

    @Test
    void allTiersStoreOnlyGenericStarlightWithTheAdvertisedCapacity() {
        QIOResourceTypeRegistry.INSTANCE.createOrLoad(temporaryDirectory.toFile());
        UUID starlight = QIOResourceTypeRegistry.INSTANCE.getOrTrack(QIOStorageDescriptors.starlight());
        UUID mana = QIOResourceTypeRegistry.INSTANCE.getOrTrack(QIOStorageDescriptors.mana());
        long[] capacities = {16_000_000L, 128_000_000L, 1_048_000_000L, 16_000_000_000_000L};
        for (QIOStorageDriveTier tier : QIOStorageDriveTier.values()) {
            QIODriveDefinition definition = tier.getDefinition(QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT);
            QIODriveRecord record = new QIODriveRecord(UUID.randomUUID(), definition, specialization);
            long capacity = capacities[tier.ordinal()];
            assertEquals(1, record.getTypeCapacity());
            assertEquals(BigInteger.valueOf(capacity), tier.getResourceCapacity(QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT));
            assertEquals(0, record.insert(mana, 1, Action.EXECUTE));
            assertEquals(capacity, record.insert(starlight, Long.MAX_VALUE, Action.SIMULATE));
            assertEquals(0, record.getTotalTypes());
            assertEquals(capacity, record.insert(starlight, Long.MAX_VALUE, Action.EXECUTE));
            assertEquals(1, record.getTotalTypes());
            assertEquals(0, record.insert(starlight, 1, Action.EXECUTE));
        }
        assertTrue(specialization.contributesToMixed());
        assertTrue(specialization.accepts(QIOStorageDescriptors.starlight()));
        assertFalse(specialization.accepts(QIOStorageDescriptors.mana()));
    }

}
