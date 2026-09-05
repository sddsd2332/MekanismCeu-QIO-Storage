package mekceuqiostorage.common.content.qio;

import mekanism.api.qio.resource.QIOResourceCodec;
import mekanism.api.qio.resource.QIOResourceCodecRegistry;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.config.QIOStorageConfig;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Scalar;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.nbt.NBTTagCompound;
import mekanism.common.content.qio.QIODriveDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QIOStorageCodecsTest {

    private static final UUID LP_OWNER = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_LP_OWNER = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @BeforeAll
    static void registerCodecs() {
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void resourceSpecificationsAreUniqueAndStable() {
        List<QIOStorageResourceSpec<?>> specs = QIOStorageResourceSpecs.all();
        assertEquals(8, specs.size());
        Set<String> modIds = new HashSet<>();
        Set<Object> codecIds = new HashSet<>();
        Set<String> families = new HashSet<>();
        Set<Object> specializationIds = new HashSet<>();
        for (QIOStorageResourceSpec<?> spec : specs) {
            modIds.add(spec.getModId());
            assertTrue(codecIds.add(spec.getCodecId()));
            assertTrue(families.add(spec.getFamily()));
            assertTrue(specializationIds.add(spec.getSpecializationId()));
            assertEquals(QIOStorage.MODID, spec.getCodecId().getNamespace());
            assertEquals(QIOStorageConfig.CODEC_VERSION, spec.getCodec().getCodecVersion());
            assertEquals(QIOStorageConfig.DEFAULT_STORAGE_UNITS_PER_UNIT,
                    spec.getStorageUnitsPerUnit());
            assertTrue(spec.contributesToMixed());
            assertSame(spec.getCodec(), QIOResourceCodecRegistry.INSTANCE.get(spec.getCodecId()));
        }
        assertEquals(7, modIds.size());
    }

    @Test
    void forgeOrderingKeepsOnlyMekanismRequired() {
        assertTrue(QIOStorage.FORGE_DEPENDENCIES.startsWith("required-after:mekanism"));
        for (QIOStorageResourceSpec<?> spec : QIOStorageResourceSpecs.all()) {
            assertTrue(QIOStorage.FORGE_DEPENDENCIES.contains(";after:" + spec.getModId()));
            assertFalse(QIOStorage.FORGE_DEPENDENCIES.contains("required-after:" + spec.getModId()));
        }
    }

    @Test
    void scalarCodecsUseOneCanonicalIdentity() {
        assertScalarRoundTrip(QIOStorageCodecs.BOTANIA_MANA, QIOStorageResources.MANA);
        assertScalarRoundTrip(QIOStorageCodecs.ASTRALSORCERY_STARLIGHT, QIOStorageResources.STARLIGHT);
        assertScalarRoundTrip(QIOStorageCodecs.EMBERS_EMBER, QIOStorageResources.EMBER);
        assertScalarRoundTrip(QIOStorageCodecs.NATURESAURA_AURA, QIOStorageResources.AURA);
        assertScalarRoundTrip(QIOStorageCodecs.PNEUMATICCRAFT_AIR, QIOStorageResources.AIR);
    }

    @Test
    void soulNetworkLpIdentityIsTheOwnerUuid() {
        QIOResourceCodec<SoulNetworkLP> codec = QIOStorageCodecs.BLOODMAGIC_LP;
        SoulNetworkLP owner = SoulNetworkLP.of(LP_OWNER, "Alice");
        SoulNetworkLP sameOwner = SoulNetworkLP.of(LP_OWNER, "AliceRenamed");
        SoulNetworkLP otherOwner = SoulNetworkLP.of(OTHER_LP_OWNER, "Bob");

        assertTrue(codec.sameType(owner, sameOwner));
        assertFalse(codec.sameType(owner, otherOwner));
        assertEquals(codec.typeHash(owner), codec.typeHash(sameOwner));

        QIOResourceDescriptor descriptor = QIOResourceDescriptor.of(codec, owner);
        QIOResourceDescriptor restored = QIOResourceDescriptor.read(descriptor.write());
        assertEquals(descriptor, restored);
        assertEquals(owner, restored.resolve(codec));
        assertEquals("Alice", restored.resolve(codec).getOwnerName());
    }

    @Test
    void onlyEssentiaUsesTheDriveDefinitionTypeCapacity() {
        assertFalse(QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA.hasFixedTypeCapacity());
        assertEquals(QIODriveDefinition.BASE.getMaxTypes(),
              QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA.getTypeCapacity(QIODriveDefinition.BASE));
        for (QIOStorageResourceSpec<?> spec : QIOStorageResourceSpecs.all()) {
            if (spec != QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA) {
                assertTrue(spec.hasFixedTypeCapacity());
                assertEquals(spec == QIOStorageResourceSpecs.BLOODMAGIC_WILL ? 5 : 1,
                      spec.getTypeCapacity(QIODriveDefinition.SUPERMASSIVE));
            }
        }
    }

    @Test
    void essentiaIdentityIsTheNormalizedAspectTag() {
        QIOResourceCodec<Essentia> codec = QIOStorageCodecs.THAUMCRAFT_ESSENTIA;
        Essentia aer = Essentia.of("aer");
        Essentia terra = Essentia.of("terra");
        assertTrue(codec.sameType(aer, Essentia.of("aer")));
        assertFalse(codec.sameType(aer, terra));
        assertEquals(codec.typeHash(aer), codec.typeHash(Essentia.of("aer")));

        QIOResourceDescriptor descriptor = QIOStorageDescriptors.essentia("aer");
        QIOResourceDescriptor restored = QIOResourceDescriptor.read(descriptor.write());
        assertEquals(descriptor, restored);
        assertEquals(aer, restored.resolve(codec));
    }

    @Test
    void willCodecPreservesAllFiveDistinctTypesAtVersionOne() {
        Set<QIOResourceDescriptor> resources = new HashSet<>();
        for (DemonWill type : DemonWill.values()) {
            QIOResourceDescriptor descriptor = QIOStorageDescriptors.will(type);
            QIOResourceDescriptor restored = QIOResourceDescriptor.read(descriptor.write());
            assertEquals(descriptor, restored);
            assertSame(type, restored.resolve(QIOStorageCodecs.BLOODMAGIC_WILL));
            assertEquals(type.getKey(), QIOStorageCodecs.BLOODMAGIC_WILL.writeTemplate(type).getString("key"));
            resources.add(restored);
        }
        assertEquals(5, resources.size());
        assertEquals(1, QIOStorageCodecs.BLOODMAGIC_WILL.getCodecVersion());
        for (String invalid : new String[]{"raw", "DEFAULT", "lp", "unknown"}) {
            NBTTagCompound payload = new NBTTagCompound();
            payload.setString("key", invalid);
            assertThrows(IllegalArgumentException.class, () -> QIOStorageCodecs.BLOODMAGIC_WILL.readTemplate(payload, 1));
        }
        NBTTagCompound payload = QIOStorageCodecs.BLOODMAGIC_WILL.writeTemplate(DemonWill.DEFAULT);
        assertThrows(IllegalArgumentException.class, () -> QIOStorageCodecs.BLOODMAGIC_WILL.readTemplate(payload, 2));
        payload.setDouble("amount", 1);
        assertThrows(IllegalArgumentException.class, () -> QIOStorageCodecs.BLOODMAGIC_WILL.readTemplate(payload, 1));
    }

    @Test
    void malformedPayloadsAndUnknownVersionsAreRejected() {
        NBTTagCompound missingKey = new NBTTagCompound();
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.BOTANIA_MANA.readTemplate(missingKey, 1));

        NBTTagCompound wrongKeyType = new NBTTagCompound();
        wrongKeyType.setInteger("key", 1);
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.BOTANIA_MANA.readTemplate(wrongKeyType, 1));

        NBTTagCompound unknownVersion = new NBTTagCompound();
        unknownVersion.setString("key", "mana");
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.BOTANIA_MANA.readTemplate(unknownVersion, 2));

        NBTTagCompound wrongScalar = new NBTTagCompound();
        wrongScalar.setString("key", "lp");
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.BOTANIA_MANA.readTemplate(wrongScalar, 1));

        NBTTagCompound extraField = new NBTTagCompound();
        extraField.setString("key", "mana");
        extraField.setString("unexpected", "data");
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.BOTANIA_MANA.readTemplate(extraField, 1));

        NBTTagCompound malformedAspect = new NBTTagCompound();
        malformedAspect.setString("key", "AER");
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.THAUMCRAFT_ESSENTIA.readTemplate(malformedAspect, 1));

        NBTTagCompound ownerlessLp = new NBTTagCompound();
        ownerlessLp.setString("key", "lp");
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.BLOODMAGIC_LP.readTemplate(ownerlessLp, 1));

        NBTTagCompound missingOwnerHalf = new NBTTagCompound();
        missingOwnerHalf.setLong("ownerMost", LP_OWNER.getMostSignificantBits());
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.BLOODMAGIC_LP.readTemplate(missingOwnerHalf,
                      QIOStorageConfig.CODEC_VERSION));

        NBTTagCompound extraOwnerField = new NBTTagCompound();
        extraOwnerField.setUniqueId("owner", LP_OWNER);
        extraOwnerField.setString("ownerName", "Alice");
        extraOwnerField.setString("unexpected", "data");
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.BLOODMAGIC_LP.readTemplate(extraOwnerField,
                      QIOStorageConfig.CODEC_VERSION));

        NBTTagCompound invalidOwnerName = new NBTTagCompound();
        invalidOwnerName.setUniqueId("owner", LP_OWNER);
        invalidOwnerName.setString("ownerName", "Alice\nBob");
        assertThrows(IllegalArgumentException.class, () ->
                QIOStorageCodecs.BLOODMAGIC_LP.readTemplate(invalidOwnerName,
                      QIOStorageConfig.CODEC_VERSION));
    }

    @Test
    void invalidResourceKeysAreRejected() {
        assertThrows(NullPointerException.class, () -> Scalar.of(null));
        assertThrows(IllegalArgumentException.class, () -> Scalar.of(""));
        assertThrows(IllegalArgumentException.class, () -> Scalar.of("Mana"));
        assertThrows(IllegalArgumentException.class, () -> Essentia.of("aer:bad"));
        assertThrows(IllegalArgumentException.class, () ->
                Essentia.of(new String(new char[129]).replace('\0', 'a')));
        assertThrows(NullPointerException.class, () -> SoulNetworkLP.of(null));
        assertThrows(IllegalArgumentException.class, () -> SoulNetworkLP.of(new UUID(0, 0)));
    }

    private static void assertScalarRoundTrip(QIOResourceCodec<Scalar> codec, Scalar resource) {
        assertSame(resource, codec.normalize(Scalar.of(resource.getKey())));
        assertTrue(codec.sameType(resource, Scalar.of(resource.getKey())));
        assertEquals(codec.typeHash(resource), codec.typeHash(Scalar.of(resource.getKey())));

        QIOResourceDescriptor descriptor = QIOResourceDescriptor.of(codec, resource);
        QIOResourceDescriptor restored = QIOResourceDescriptor.read(descriptor.write());
        assertEquals(descriptor, restored);
        assertNotNull(restored.resolve(codec));
        assertSame(resource, restored.resolve(codec));
    }
}
