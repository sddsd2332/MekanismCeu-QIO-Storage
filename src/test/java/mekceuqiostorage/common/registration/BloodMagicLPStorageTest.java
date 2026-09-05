package mekceuqiostorage.common.registration;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.common.content.qio.QIODriveDefinition;
import mekanism.common.content.qio.QIODriveRecord;
import mekanism.common.content.qio.QIODriveSpecialization;
import mekanism.common.content.qio.QIODriveSpecializationRegistry;
import mekanism.common.content.qio.QIODriveSpecializations;
import mekanism.common.content.qio.QIOResourceTypeRegistry;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMagicLPStorageTest {

    private static final UUID ALICE = UUID.fromString(
          "10000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString(
          "20000000-0000-0000-0000-000000000002");

    private static QIODriveSpecialization lpSpecialization;

    @BeforeAll
    static void registerLpStorage() {
        QIOStorageBootstrap.registerCodecs();
        lpSpecialization = QIODriveSpecializationRegistry.INSTANCE.get(
              QIOStorageResourceSpecs.BLOODMAGIC_LP.getSpecializationId());
        if (lpSpecialization == null) {
            lpSpecialization = QIOStorageBootstrap.registerSpecialization(
                  QIOStorageResourceSpecs.BLOODMAGIC_LP);
        }
    }

    @AfterEach
    void resetResourceTypes() {
        QIOResourceTypeRegistry.INSTANCE.reset();
    }

    @TempDir
    Path temporaryDirectory;

    @Test
    void dedicatedDriveStoresOneOwnerWhileMixedDriveStoresMultipleOwners() {
        QIOResourceTypeRegistry.INSTANCE.createOrLoad(temporaryDirectory.toFile());
        QIOResourceDescriptor alice = descriptor(ALICE, "Alice");
        QIOResourceDescriptor renamedAlice = descriptor(ALICE, "AliceRenamed");
        QIOResourceDescriptor bob = descriptor(BOB, "Bob");
        UUID aliceType = QIOResourceTypeRegistry.INSTANCE.getOrTrack(alice);
        UUID renamedAliceType = QIOResourceTypeRegistry.INSTANCE.getOrTrack(renamedAlice);
        UUID bobType = QIOResourceTypeRegistry.INSTANCE.getOrTrack(bob);

        assertEquals(aliceType, renamedAliceType);
        assertNotEquals(aliceType, bobType);
        assertTrue(lpSpecialization.accepts(alice));
        assertTrue(lpSpecialization.accepts(bob));

        QIODriveDefinition dedicatedDefinition = QIOStorageDriveTier.BASE.getDefinition(
              QIOStorageResourceSpecs.BLOODMAGIC_LP);
        QIODriveRecord dedicated = new QIODriveRecord(UUID.randomUUID(), dedicatedDefinition,
              lpSpecialization);
        assertEquals(1, dedicated.getTypeCapacity());
        assertEquals(100, dedicated.insert(aliceType, 100, Action.EXECUTE));
        assertEquals(25, dedicated.insert(renamedAliceType, 25, Action.EXECUTE));
        assertEquals(0, dedicated.insert(bobType, 100, Action.SIMULATE));
        assertEquals(0, dedicated.insert(bobType, 100, Action.EXECUTE));
        assertEquals(1, dedicated.getTotalTypes());

        QIODriveRecord mixed = new QIODriveRecord(UUID.randomUUID(), QIODriveDefinition.BASE,
              QIODriveSpecializations.MIXED);
        assertEquals(100, mixed.insert(aliceType, 100, Action.EXECUTE));
        assertEquals(100, mixed.insert(bobType, 100, Action.EXECUTE));
        assertEquals(2, mixed.getTotalTypes());
    }

    @Test
    void allDedicatedLpTiersUseOneTypeAndOneStorageUnitPerLp() {
        assertTrue(lpSpecialization.contributesToMixed());
        assertLpTier(QIOStorageDriveTier.BASE, 16_000_000L);
        assertLpTier(QIOStorageDriveTier.HYPER_DENSE, 128_000_000L);
        assertLpTier(QIOStorageDriveTier.TIME_DILATING, 1_048_000_000L);
        assertLpTier(QIOStorageDriveTier.SUPERMASSIVE, 16_000_000_000_000L);
    }

    private static void assertLpTier(QIOStorageDriveTier tier, long lpCapacity) {
        QIODriveDefinition definition = tier.getDefinition(
              QIOStorageResourceSpecs.BLOODMAGIC_LP);
        assertEquals(1, definition.getMaxTypes());
        assertEquals(tier.getItemEquivalentCapacity(),
              lpSpecialization.getCountCapacity(definition));
        assertEquals(BigInteger.valueOf(lpCapacity),
              tier.getResourceCapacity(QIOStorageResourceSpecs.BLOODMAGIC_LP));
    }

    private static QIOResourceDescriptor descriptor(UUID ownerId, String ownerName) {
        return QIOResourceDescriptor.of(QIOStorageResourceSpecs.BLOODMAGIC_LP.getCodec(),
              SoulNetworkLP.of(ownerId, ownerName));
    }
}
