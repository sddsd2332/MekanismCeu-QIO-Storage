package mekceuqiostorage.common.content.qio;

import mekanism.api.qio.resource.QIOResourceCodec;
import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.config.QIOStorageConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants.NBT;

import javax.annotation.Nonnull;
import java.util.Objects;

import static mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import static mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import static mekceuqiostorage.common.content.qio.QIOStorageResources.Scalar;
import static mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;

/** Stable codec definitions for all resources supplied by this addon. */
public final class QIOStorageCodecs {

    public static final ResourceLocation THAUMCRAFT_ESSENTIA_ID = id("thaumcraft_essentia");
    public static final ResourceLocation BOTANIA_MANA_ID = id("botania_mana");
    public static final ResourceLocation BLOODMAGIC_LP_ID = id("bloodmagic_lp");
    public static final ResourceLocation BLOODMAGIC_WILL_ID = id("bloodmagic_will");
    public static final ResourceLocation ASTRALSORCERY_STARLIGHT_ID = id("astralsorcery_starlight");
    public static final ResourceLocation EMBERS_EMBER_ID = id("embers_ember");
    public static final ResourceLocation NATURESAURA_AURA_ID = id("naturesaura_aura");
    public static final ResourceLocation PNEUMATICCRAFT_AIR_ID = id("pneumaticcraft_air");

    /* Families are deliberately un-namespaced (the Mekanism API validates this format). */
    public static final String THAUMCRAFT_ESSENTIA_FAMILY = "thaumcraft_essentia";
    public static final String BOTANIA_MANA_FAMILY = "botania_mana";
    public static final String BLOODMAGIC_LP_FAMILY = "bloodmagic_lp";
    public static final String BLOODMAGIC_WILL_FAMILY = "bloodmagic_will";
    public static final String ASTRALSORCERY_STARLIGHT_FAMILY = "astralsorcery_starlight";
    public static final String EMBERS_EMBER_FAMILY = "embers_ember";
    public static final String NATURESAURA_AURA_FAMILY = "naturesaura_aura";
    public static final String PNEUMATICCRAFT_AIR_FAMILY = "pneumaticcraft_air";

    public static final QIOResourceCodec<Essentia> THAUMCRAFT_ESSENTIA =
          new EssentiaCodec(THAUMCRAFT_ESSENTIA_ID, THAUMCRAFT_ESSENTIA_FAMILY);
    public static final QIOResourceCodec<Scalar> BOTANIA_MANA =
          new ScalarCodec(BOTANIA_MANA_ID, BOTANIA_MANA_FAMILY, QIOStorageResources.MANA);
    public static final QIOResourceCodec<SoulNetworkLP> BLOODMAGIC_LP =
          new SoulNetworkLPCodec(BLOODMAGIC_LP_ID, BLOODMAGIC_LP_FAMILY);
    public static final QIOResourceCodec<DemonWill> BLOODMAGIC_WILL =
          new DemonWillCodec(BLOODMAGIC_WILL_ID, BLOODMAGIC_WILL_FAMILY);
    public static final QIOResourceCodec<Scalar> ASTRALSORCERY_STARLIGHT =
          new ScalarCodec(ASTRALSORCERY_STARLIGHT_ID, ASTRALSORCERY_STARLIGHT_FAMILY, QIOStorageResources.STARLIGHT);
    public static final QIOResourceCodec<Scalar> EMBERS_EMBER =
          new ScalarCodec(EMBERS_EMBER_ID, EMBERS_EMBER_FAMILY, QIOStorageResources.EMBER);
    public static final QIOResourceCodec<Scalar> NATURESAURA_AURA =
          new ScalarCodec(NATURESAURA_AURA_ID, NATURESAURA_AURA_FAMILY, QIOStorageResources.AURA);
    public static final QIOResourceCodec<Scalar> PNEUMATICCRAFT_AIR =
          new ScalarCodec(PNEUMATICCRAFT_AIR_ID, PNEUMATICCRAFT_AIR_FAMILY, QIOStorageResources.AIR);

    private QIOStorageCodecs() {
    }

    @Nonnull
    private static ResourceLocation id(String path) {
        return new ResourceLocation(QIOStorage.MODID, path);
    }

    private abstract static class BaseCodec<T> implements QIOResourceCodec<T> {

        private final ResourceLocation id;
        private final String family;

        private BaseCodec(ResourceLocation id, String family) {
            this.id = Objects.requireNonNull(id, "codec id");
            this.family = Objects.requireNonNull(family, "codec family");
        }

        @Override
        @Nonnull
        public ResourceLocation getCodecId() {
            return id;
        }

        @Override
        @Nonnull
        public String getFamily() {
            return family;
        }

        @Override
        public int getCodecVersion() {
            return QIOStorageConfig.CODEC_VERSION;
        }

        @Override
        public long getStorageUnitsPerUnit() {
            // One codec unit occupies one storage unit, including one whole Demon Will.
            return QIOStorageConfig.DEFAULT_STORAGE_UNITS_PER_UNIT;
        }

        protected static void requirePayload(NBTTagCompound payload, int version) {
            Objects.requireNonNull(payload, "QIO resource payload cannot be null");
            if (version != QIOStorageConfig.CODEC_VERSION) {
                throw new IllegalArgumentException("Unsupported QIO resource codec version: " + version);
            }
            if (!payload.hasKey("key", NBT.TAG_STRING)) {
                throw new IllegalArgumentException("QIO resource payload has no key");
            }
            if (payload.getKeySet().size() != 1) {
                throw new IllegalArgumentException("QIO resource payload contains unexpected fields");
            }
            String key = payload.getString("key");
            QIOStorageResources.requireKey(key);
        }
    }

    private static final class ScalarCodec extends BaseCodec<Scalar> {

        private final Scalar template;

        private ScalarCodec(ResourceLocation id, String family, Scalar template) {
            super(id, family);
            this.template = Objects.requireNonNull(template, "scalar resource template");
        }

        @Override
        @Nonnull
        public Class<Scalar> getValueClass() {
            return Scalar.class;
        }

        @Override
        @Nonnull
        public Scalar normalize(@Nonnull Scalar value) {
            Objects.requireNonNull(value, "QIO scalar resource cannot be null");
            if (!template.getKey().equals(value.getKey())) {
                throw new IllegalArgumentException("Unexpected resource key for " + getCodecId() +
                      ": " + value.getKey());
            }
            return template;
        }

        @Override
        public boolean sameType(@Nonnull Scalar first, @Nonnull Scalar second) {
            return normalize(first).equals(normalize(second));
        }

        @Override
        public int typeHash(@Nonnull Scalar value) {
            return normalize(value).hashCode();
        }

        @Override
        @Nonnull
        public NBTTagCompound writeTemplate(@Nonnull Scalar value) {
            NBTTagCompound payload = new NBTTagCompound();
            payload.setString("key", normalize(value).getKey());
            return payload;
        }

        @Override
        @Nonnull
        public Scalar readTemplate(@Nonnull NBTTagCompound payload, int codecVersion) {
            requirePayload(payload, codecVersion);
            String payloadKey = payload.getString("key");
            if (!template.getKey().equals(payloadKey)) {
                throw new IllegalArgumentException("Unexpected resource key for " + getCodecId() +
                      ": " + payloadKey);
            }
            return template;
        }
    }

    private static final class EssentiaCodec extends BaseCodec<Essentia> {

        private EssentiaCodec(ResourceLocation id, String family) {
            super(id, family);
        }

        @Override
        @Nonnull
        public Class<Essentia> getValueClass() {
            return Essentia.class;
        }

        @Override
        @Nonnull
        public Essentia normalize(@Nonnull Essentia value) {
            Objects.requireNonNull(value, "QIO essentia resource cannot be null");
            return Essentia.of(value.getAspectTag());
        }

        @Override
        public boolean sameType(@Nonnull Essentia first, @Nonnull Essentia second) {
            return normalize(first).equals(normalize(second));
        }

        @Override
        public int typeHash(@Nonnull Essentia value) {
            return normalize(value).hashCode();
        }

        @Override
        @Nonnull
        public NBTTagCompound writeTemplate(@Nonnull Essentia value) {
            NBTTagCompound payload = new NBTTagCompound();
            payload.setString("key", normalize(value).getAspectTag());
            return payload;
        }

        @Override
        @Nonnull
        public Essentia readTemplate(@Nonnull NBTTagCompound payload, int codecVersion) {
            requirePayload(payload, codecVersion);
            return Essentia.of(payload.getString("key"));
        }
    }

    private static final class DemonWillCodec extends BaseCodec<DemonWill> {

        private DemonWillCodec(ResourceLocation id, String family) {
            super(id, family);
        }

        @Override
        @Nonnull
        public Class<DemonWill> getValueClass() {
            return DemonWill.class;
        }

        @Override
        @Nonnull
        public DemonWill normalize(@Nonnull DemonWill value) {
            return Objects.requireNonNull(value, "Demon Will type");
        }

        @Override
        public boolean sameType(@Nonnull DemonWill first, @Nonnull DemonWill second) {
            return normalize(first) == normalize(second);
        }

        @Override
        public int typeHash(@Nonnull DemonWill value) {
            return normalize(value).getKey().hashCode();
        }

        @Override
        @Nonnull
        public NBTTagCompound writeTemplate(@Nonnull DemonWill value) {
            NBTTagCompound payload = new NBTTagCompound();
            payload.setString("key", normalize(value).getKey());
            return payload;
        }

        @Override
        @Nonnull
        public DemonWill readTemplate(@Nonnull NBTTagCompound payload, int codecVersion) {
            requirePayload(payload, codecVersion);
            return DemonWill.fromKey(payload.getString("key"));
        }
    }

    private static final class SoulNetworkLPCodec extends BaseCodec<SoulNetworkLP> {

        private static final String OWNER = "owner";
        private static final String OWNER_MOST = OWNER + "Most";
        private static final String OWNER_LEAST = OWNER + "Least";
        private static final String OWNER_NAME = "ownerName";

        private SoulNetworkLPCodec(ResourceLocation id, String family) {
            super(id, family);
        }

        @Override
        @Nonnull
        public Class<SoulNetworkLP> getValueClass() {
            return SoulNetworkLP.class;
        }

        @Override
        @Nonnull
        public SoulNetworkLP normalize(@Nonnull SoulNetworkLP value) {
            Objects.requireNonNull(value, "QIO Soul Network LP resource cannot be null");
            return SoulNetworkLP.of(value.getOwnerId(), value.getOwnerName());
        }

        @Override
        public boolean sameType(@Nonnull SoulNetworkLP first, @Nonnull SoulNetworkLP second) {
            return normalize(first).equals(normalize(second));
        }

        @Override
        public int typeHash(@Nonnull SoulNetworkLP value) {
            return normalize(value).hashCode();
        }

        @Override
        @Nonnull
        public NBTTagCompound writeTemplate(@Nonnull SoulNetworkLP value) {
            NBTTagCompound payload = new NBTTagCompound();
            SoulNetworkLP normalized = normalize(value);
            payload.setUniqueId(OWNER, normalized.getOwnerId());
            payload.setString(OWNER_NAME, normalized.getOwnerName() == null ? "" :
                  normalized.getOwnerName());
            return payload;
        }

        @Override
        @Nonnull
        public SoulNetworkLP readTemplate(@Nonnull NBTTagCompound payload, int codecVersion) {
            Objects.requireNonNull(payload, "QIO Soul Network LP payload cannot be null");
            if (codecVersion != QIOStorageConfig.CODEC_VERSION) {
                throw new IllegalArgumentException("Unsupported Soul Network LP codec version: " +
                      codecVersion);
            }
            if (!payload.hasKey(OWNER_MOST, NBT.TAG_LONG) ||
                  !payload.hasKey(OWNER_LEAST, NBT.TAG_LONG) ||
                  !payload.hasKey(OWNER_NAME, NBT.TAG_STRING) ||
                  payload.getKeySet().size() != 3) {
                throw new IllegalArgumentException("Invalid Soul Network LP owner payload");
            }
            String storedName = payload.getString(OWNER_NAME);
            SoulNetworkLP resource = SoulNetworkLP.of(payload.getUniqueId(OWNER), storedName);
            if (!storedName.isEmpty() && !storedName.equals(resource.getOwnerName())) {
                throw new IllegalArgumentException("Invalid Soul Network LP owner name");
            }
            return resource;
        }
    }
}
