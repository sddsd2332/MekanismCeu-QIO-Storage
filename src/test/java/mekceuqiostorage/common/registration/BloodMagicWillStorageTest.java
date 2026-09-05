package mekceuqiostorage.common.registration;

import mekanism.api.Action;
import mekanism.common.content.qio.QIODriveDefinition;
import mekanism.common.content.qio.QIODriveRecord;
import mekanism.common.content.qio.QIODriveSpecialization;
import mekanism.common.content.qio.QIODriveSpecializationRegistry;
import mekanism.common.content.qio.QIOResourceTypeRegistry;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMagicWillStorageTest {

    private static QIODriveSpecialization willSpecialization;
    private static QIODriveSpecialization lpSpecialization;

    @TempDir
    Path directory;

    @BeforeAll
    static void register() {
        QIOStorageBootstrap.registerCodecs();
        willSpecialization = QIODriveSpecializationRegistry.INSTANCE.get(QIOStorageResourceSpecs.BLOODMAGIC_WILL.getSpecializationId());
        if (willSpecialization == null) {
            willSpecialization = QIOStorageBootstrap.registerSpecialization(QIOStorageResourceSpecs.BLOODMAGIC_WILL);
        }
        lpSpecialization = QIODriveSpecializationRegistry.INSTANCE.get(QIOStorageResourceSpecs.BLOODMAGIC_LP.getSpecializationId());
        if (lpSpecialization == null) {
            lpSpecialization = QIOStorageBootstrap.registerSpecialization(QIOStorageResourceSpecs.BLOODMAGIC_LP);
        }
    }

    @AfterEach
    void resetTypes() {
        QIOResourceTypeRegistry.INSTANCE.reset();
    }

    @Test
    void willAndLpUseIndependentDefinitionsAndRejectEachOther() {
        QIOResourceTypeRegistry.INSTANCE.createOrLoad(directory.toFile());
        UUID lp = QIOResourceTypeRegistry.INSTANCE.getOrTrack(QIOStorageDescriptors.lp(UUID.randomUUID()));
        UUID will = QIOResourceTypeRegistry.INSTANCE.getOrTrack(QIOStorageDescriptors.will(DemonWill.DEFAULT));
        for (QIOStorageDriveTier tier : QIOStorageDriveTier.values()) {
            QIODriveDefinition willDefinition = tier.getDefinition(QIOStorageResourceSpecs.BLOODMAGIC_WILL);
            QIODriveDefinition lpDefinition = tier.getDefinition(QIOStorageResourceSpecs.BLOODMAGIC_LP);
            assertNotEquals(lpDefinition, willDefinition);
            assertSame(tier, QIOStorageDriveTier.fromDefinition(willDefinition));
            QIODriveRecord willDrive = new QIODriveRecord(UUID.randomUUID(), willDefinition, willSpecialization);
            QIODriveRecord lpDrive = new QIODriveRecord(UUID.randomUUID(), lpDefinition, lpSpecialization);
            assertEquals(5, willDrive.getTypeCapacity());
            assertEquals(1, lpDrive.getTypeCapacity());
            assertEquals(0, willDrive.insert(lp, 1, Action.EXECUTE));
            assertEquals(0, lpDrive.insert(will, 1, Action.EXECUTE));
            assertEquals(25, willDrive.insert(will, 25, Action.EXECUTE));
            assertEquals(25, lpDrive.insert(lp, 25, Action.EXECUTE));
        }
        assertTrue(willSpecialization.contributesToMixed());
        assertFalse(willSpecialization.accepts(QIOStorageDescriptors.mana()));
    }

    @Test
    void allFiveWillTypesShareOneCapacity() {
        QIOResourceTypeRegistry.INSTANCE.createOrLoad(directory.toFile());
        for (QIOStorageDriveTier tier : QIOStorageDriveTier.values()) {
            QIODriveRecord drive = new QIODriveRecord(UUID.randomUUID(),
                  tier.getDefinition(QIOStorageResourceSpecs.BLOODMAGIC_WILL), willSpecialization);
            UUID first = null;
            for (DemonWill type : DemonWill.values()) {
                UUID id = QIOResourceTypeRegistry.INSTANCE.getOrTrack(QIOStorageDescriptors.will(type));
                if (first == null) {
                    first = id;
                }
                assertEquals(29, drive.insert(id, 29, Action.EXECUTE));
            }
            assertEquals(5, drive.getTotalTypes());
            long capacity = tier.getResourceCapacity(QIOStorageResourceSpecs.BLOODMAGIC_WILL).longValueExact();
            assertEquals(capacity - 145, drive.insert(first, Long.MAX_VALUE, Action.EXECUTE));
            assertEquals(0, drive.insert(first, 1, Action.EXECUTE));
            assertEquals(capacity, drive.getTotalStorageUnits());
        }
    }
}
