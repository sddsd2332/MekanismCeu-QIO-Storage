package mekceuqiostorage.common.registration;

import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.common.content.qio.QIOAmount;
import mekanism.common.content.qio.QIODriveDefinition;
import mekanism.common.content.qio.QIODriveRecord;
import mekanism.common.content.qio.QIODriveSpecialization;
import mekanism.common.content.qio.QIODriveSpecializations;
import mekceuqiostorage.common.content.qio.QIOStorageCodecs;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QIOStorageDriveTierTest {

    private static final UUID LP_OWNER = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void specializationAcceptsOnlyItsCodecAndContributesExactCapacity() {
        QIOStorageBootstrap.registerCodecs();
        QIOAmount before = QIODriveSpecializations.MIXED.getExactCountCapacity(
              QIODriveDefinition.BASE);
        QIOStorageResourceSpec<QIOStorageResources.Scalar> spec = new QIOStorageResourceSpec<>(
              "testmod", QIOStorageCodecs.BOTANIA_MANA, "test_mana_contribution",
              "qio.mekceuqiostorage.test_mana_contribution", true);

        QIODriveSpecialization specialization = QIOStorageBootstrap.registerSpecialization(spec);
        QIOResourceDescriptor mana = QIOStorageDescriptors.mana();
        QIOResourceDescriptor lp = QIOStorageDescriptors.lp(LP_OWNER);

        assertTrue(specialization.accepts(mana));
        assertFalse(specialization.accepts(lp));
        assertEquals(QIODriveDefinition.BASE.getMaxCount(),
              specialization.getCountCapacity(QIODriveDefinition.BASE));
        assertEquals(before.add(QIODriveDefinition.BASE.getMaxCount()),
              QIODriveSpecializations.MIXED.getExactCountCapacity(QIODriveDefinition.BASE));
    }

    @Test
    void excludedSpecializationDoesNotIncreaseMixedCapacity() {
        QIOStorageBootstrap.registerCodecs();
        QIOAmount before = QIODriveSpecializations.MIXED.getExactCountCapacity(
              QIODriveDefinition.HYPER_DENSE);
        QIOStorageResourceSpec<QIOStorageResources.SoulNetworkLP> spec = new QIOStorageResourceSpec<>(
              "testmod", QIOStorageCodecs.BLOODMAGIC_LP, "test_lp_excluded",
              "qio.mekceuqiostorage.test_lp_excluded", false);

        QIODriveSpecialization specialization = QIOStorageBootstrap.registerSpecialization(spec);

        assertFalse(specialization.contributesToMixed());
        assertEquals(before, QIODriveSpecializations.MIXED.getExactCountCapacity(
              QIODriveDefinition.HYPER_DENSE));
    }

    @Test
    void mirrorsMekanismDriveDefinitions() {
        assertTier(QIOStorageDriveTier.BASE, QIODriveDefinition.BASE,
              16_000L, 128, 16_000_000L);
        assertTier(QIOStorageDriveTier.HYPER_DENSE, QIODriveDefinition.HYPER_DENSE,
              128_000L, 256, 128_000_000L);
        assertTier(QIOStorageDriveTier.TIME_DILATING, QIODriveDefinition.TIME_DILATING,
              1_048_000L, 1_024, 1_048_000_000L);
        assertTier(QIOStorageDriveTier.SUPERMASSIVE, QIODriveDefinition.SUPERMASSIVE,
              16_000_000_000L, 8_192, 16_000_000_000_000L);
    }

    @Test
    void scalarDefinitionsPersistOneTypeInDriveRecords() {
        QIOStorageBootstrap.registerCodecs();
        QIOStorageResourceSpec<QIOStorageResources.Scalar> spec = new QIOStorageResourceSpec<>(
              "testmod", QIOStorageCodecs.EMBERS_EMBER, "test_single_type_record",
              "qio.mekceuqiostorage.test_single_type_record", true, 1);
        QIODriveSpecialization specialization = QIOStorageBootstrap.registerSpecialization(spec);

        for (QIOStorageDriveTier tier : QIOStorageDriveTier.values()) {
            QIODriveDefinition definition = tier.getDefinition(spec);
            QIODriveRecord record = new QIODriveRecord(UUID.randomUUID(), definition,
                  specialization);

            assertTrue(QIODriveDefinition.isRegistered(definition));
            assertSame(tier, QIOStorageDriveTier.fromDefinition(definition));
            assertEquals(tier.getItemEquivalentCapacity(), definition.getMaxCount());
            assertEquals(1, definition.getMaxTypes());
            assertEquals(1, record.getTypeCapacity());
        }
    }

    private static void assertTier(QIOStorageDriveTier tier, QIODriveDefinition definition,
          long itemEquivalentCapacity, int typeCapacity, long resourceCapacity) {
        assertSame(definition, tier.getDefinition());
        assertSame(tier, QIOStorageDriveTier.fromDefinition(definition));
        assertEquals(itemEquivalentCapacity, tier.getItemEquivalentCapacity());
        assertEquals(typeCapacity, tier.getTypeCapacity());
        assertEquals(typeCapacity,
              tier.getTypeCapacity(QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA));
        assertEquals(1, tier.getTypeCapacity(QIOStorageResourceSpecs.BOTANIA_MANA));
        assertSame(definition,
              tier.getDefinition(QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA));
        assertEquals(1,
              tier.getDefinition(QIOStorageResourceSpecs.BOTANIA_MANA).getMaxTypes());
        assertEquals(BigInteger.valueOf(resourceCapacity),
              tier.getResourceCapacity(QIOStorageResourceSpecs.BOTANIA_MANA));
    }
}
