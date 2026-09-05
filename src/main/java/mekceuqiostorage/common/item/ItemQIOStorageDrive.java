package mekceuqiostorage.common.item;

import mekanism.api.EnumColor;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.api.qio.resource.QIOResourceFamilyMatcher;
import mekanism.common.MekanismLang;
import mekanism.common.content.qio.QIODriveRecord;
import mekanism.common.content.qio.QIODriveSpecialization;
import mekanism.common.content.qio.QIODriveStorage;
import mekanism.common.content.qio.QIOResourceTypeRegistry;
import mekanism.common.tier.QIODriveTier;
import mekanism.common.item.ItemQIODrive;
import mekanism.common.util.LangUtils;
import mekanism.common.util.text.TextUtils;
import mekceuqiostorage.common.config.QIOStorageConfig;
import mekceuqiostorage.common.content.qio.QIOStorageCodecs;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** QIO drive item whose tooltip reports capacity in the custom codec's whole storage units. */
public final class ItemQIOStorageDrive extends ItemQIODrive {

    /** Client-side Aspect hint metadata synchronized from a Thaumcraft drive record. */
    private static final String SINGLE_ESSENTIA_TAG = "qioSingleEssentia";
    /** Bounded list used to report multi-Aspect drives as an ambiguous selection. */
    private static final String ESSENTIA_TYPES_TAG = "qioEssentiaTypes";
    private static final int MAX_ESSENTIA_HINTS = 32;
    private static final int MAX_ESSENTIA_HINT_ENTRIES = MAX_ESSENTIA_HINTS * 2;
    private static final String SOUL_NETWORK_OWNER_TAG = "qioSoulNetworkOwner";
    private static final String SOUL_NETWORK_OWNER_NAME_TAG = "qioSoulNetworkOwnerName";
    private static final String WILL_RESOURCES_TAG = "qioWillResources";

    private final QIOStorageDriveTier tier;
    private final QIOStorageResourceSpec<?> resourceSpec;
    private final QIODriveSpecialization specialization;

    public ItemQIOStorageDrive(@Nonnull QIOStorageDriveTier tier,
          @Nonnull QIOStorageResourceSpec<?> resourceSpec,
          @Nonnull QIODriveSpecialization specialization) {
        super(Objects.requireNonNull(tier, "drive tier").getDefinition(
              Objects.requireNonNull(resourceSpec, "resource specification")), specialization);
        this.tier = tier;
        this.resourceSpec = Objects.requireNonNull(resourceSpec, "resource specification");
        this.specialization = Objects.requireNonNull(specialization, "drive specialization");
        if (!QIOResourceFamilyMatcher.codec(resourceSpec.getCodecId()).equals(
              specialization.getMatcher())) {
            throw new IllegalArgumentException("Drive specialization does not own codec " +
                  resourceSpec.getCodecId());
        }
    }

    @Nonnull
    public QIOStorageResourceSpec<?> getResourceSpec() {
        return resourceSpec;
    }

    @Nonnull
    public QIOStorageDriveTier getStorageTier() {
        return tier;
    }

    /**
     * Returns the cached single-Aspect identity, if the drive currently has exactly one type.
     * The hint is populated from the authoritative server-side QIO record and is never used for
     * transfer decisions.
     */
    @Nullable
    public String getSingleEssentiaTag(@Nonnull ItemStack stack) {
        if (getDriveMetadata(stack).getTypes() != 1) {
            return null;
        }
        List<String> tags = getEssentiaTags(stack);
        return tags.size() == 1 ? tags.get(0) : null;
    }

    /**
     * Returns the bounded, server-synchronized Aspect hints carried by this drive stack. These
     * hints are for client selection only; the QIO drive record remains authoritative.
     */
    @Nonnull
    public List<String> getEssentiaTags(@Nonnull ItemStack stack) {
        if (resourceSpec != QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA ||
              !stack.hasTagCompound()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        NBTTagList entries = stack.getTagCompound().getTagList(ESSENTIA_TYPES_TAG,
              NBT.TAG_COMPOUND);
        int entryCount = Math.min(entries.tagCount(), MAX_ESSENTIA_HINT_ENTRIES);
        for (int i = 0; i < entryCount && result.size() < MAX_ESSENTIA_HINTS; i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            if (!entry.hasKey("key", NBT.TAG_STRING)) {
                continue;
            }
            String key = entry.getString("key");
            try {
                if (seen.add(Essentia.of(key).getAspectTag())) {
                    result.add(key);
                }
            } catch (RuntimeException ignored) {
                // Ignore malformed client-side hints and keep the remaining candidates usable.
            }
        }
        if (result.isEmpty() && stack.getTagCompound().hasKey(SINGLE_ESSENTIA_TAG,
              NBT.TAG_STRING)) {
            String key = stack.getTagCompound().getString(SINGLE_ESSENTIA_TAG);
            try {
                result.add(Essentia.of(key).getAspectTag());
            } catch (RuntimeException ignored) {
                // Ignore malformed legacy hint.
            }
        }
        return result.isEmpty() ? Collections.emptyList() :
              Collections.unmodifiableList(result);
    }

    /** Returns the sole Soul Network owner cached from this populated one-type drive. */
    @Nullable
    public SoulNetworkLP getSingleSoulNetwork(@Nonnull ItemStack stack) {
        if (resourceSpec != QIOStorageResourceSpecs.BLOODMAGIC_LP ||
              getDriveMetadata(stack).getTypes() != 1 || !stack.hasTagCompound() ||
              !stack.getTagCompound().hasUniqueId(SOUL_NETWORK_OWNER_TAG)) {
            return null;
        }
        try {
            String ownerName = stack.getTagCompound().hasKey(SOUL_NETWORK_OWNER_NAME_TAG,
                  NBT.TAG_STRING) ? stack.getTagCompound().getString(
                  SOUL_NETWORK_OWNER_NAME_TAG) : null;
            return SoulNetworkLP.of(stack.getTagCompound().getUniqueId(
                  SOUL_NETWORK_OWNER_TAG), ownerName);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Bounded client selection hints for the five Will types stored in this drive. */
    @Nonnull
    public List<QIOResourceDescriptor> getWillResources(@Nonnull ItemStack stack) {
        if (resourceSpec != QIOStorageResourceSpecs.BLOODMAGIC_WILL ||
              getDriveMetadata(stack).getTypes() <= 0 || !stack.hasTagCompound()) {
            return Collections.emptyList();
        }
        Set<QIOResourceDescriptor> resources = new LinkedHashSet<>();
        NBTTagList hints = stack.getTagCompound().getTagList(WILL_RESOURCES_TAG, NBT.TAG_COMPOUND);
        int maximum = Math.min(hints.tagCount(), QIOStorageConfig.BLOODMAGIC_WILL_TYPE_CAPACITY);
        for (int i = 0; i < maximum; i++) {
            try {
                QIOResourceDescriptor descriptor = QIOResourceDescriptor.read(hints.getCompoundTagAt(i));
                if (descriptor.isResolved() && resourceSpec.getCodecId().equals(descriptor.getCodecId())) {
                    resources.add(descriptor);
                }
            } catch (RuntimeException ignored) {
                // Malformed client hints cannot make the remaining selections unavailable.
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(resources));
    }

    @Override
    public void setDriveMetadata(@Nonnull ItemStack stack, long count, int types,
          long storageUnits) {
        super.setDriveMetadata(stack, count, types, storageUnits);
        if (resourceSpec == QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA) {
            updateEssentiaHints(stack, types);
        } else if (resourceSpec == QIOStorageResourceSpecs.BLOODMAGIC_LP) {
            updateSoulNetworkOwnerHint(stack, types);
        } else if (resourceSpec == QIOStorageResourceSpecs.BLOODMAGIC_WILL) {
            updateWillHints(stack, types);
        }
    }

    private void updateEssentiaHints(ItemStack stack, int types) {
        if (types <= 0) {
            removeEssentiaHints(stack);
            return;
        }
        try {
            QIODriveRecord record = getOwnedRecord(stack);
            if (record.getTotalTypes() <= 0) {
                removeEssentiaHints(stack);
                return;
            }
            TreeSet<String> tags = new TreeSet<>();
            for (UUID resource : record.getContents().keySet()) {
                QIOResourceDescriptor descriptor = QIOResourceTypeRegistry.INSTANCE
                      .getDescriptorByUUID(resource);
                Essentia essentia = descriptor == null ? null :
                      descriptor.resolve(QIOStorageCodecs.THAUMCRAFT_ESSENTIA);
                if (essentia != null) {
                    tags.add(essentia.getAspectTag());
                    if (tags.size() > MAX_ESSENTIA_HINTS) {
                        tags.pollLast();
                    }
                }
            }
            List<String> boundedTags = new ArrayList<>(tags);
            writeEssentiaHints(stack, boundedTags);
        } catch (LinkageError | RuntimeException ignored) {
            // Metadata synchronization must never make a mounted drive unavailable.
            removeEssentiaHints(stack);
        }
    }

    private void updateSoulNetworkOwnerHint(ItemStack stack, int types) {
        if (types != 1) {
            removeSoulNetworkOwnerHint(stack);
            return;
        }
        try {
            QIODriveRecord record = getOwnedRecord(stack);
            if (record.getTotalTypes() != 1) {
                removeSoulNetworkOwnerHint(stack);
                return;
            }
            SoulNetworkLP network = null;
            for (UUID resource : record.getContents().keySet()) {
                QIOResourceDescriptor descriptor = QIOResourceTypeRegistry.INSTANCE
                      .getDescriptorByUUID(resource);
                SoulNetworkLP candidate = descriptor == null ? null :
                      descriptor.resolve(QIOStorageCodecs.BLOODMAGIC_LP);
                if (candidate != null) {
                    if (network != null && !network.equals(candidate)) {
                        removeSoulNetworkOwnerHint(stack);
                        return;
                    }
                    network = candidate;
                }
            }
            if (network == null) {
                removeSoulNetworkOwnerHint(stack);
                return;
            }
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            stack.getTagCompound().setUniqueId(SOUL_NETWORK_OWNER_TAG, network.getOwnerId());
            if (network.getOwnerName() == null) {
                stack.getTagCompound().removeTag(SOUL_NETWORK_OWNER_NAME_TAG);
            } else {
                stack.getTagCompound().setString(SOUL_NETWORK_OWNER_NAME_TAG,
                      network.getOwnerName());
            }
        } catch (LinkageError | RuntimeException ignored) {
            removeSoulNetworkOwnerHint(stack);
        }
    }

    private void updateWillHints(ItemStack stack, int types) {
        if (types <= 0) {
            removeWillHints(stack);
            return;
        }
        try {
            QIODriveRecord record = getOwnedRecord(stack);
            NBTTagList hints = new NBTTagList();
            for (UUID resource : record.getContents().keySet()) {
                QIOResourceDescriptor descriptor = QIOResourceTypeRegistry.INSTANCE
                      .getDescriptorByUUID(resource);
                if (descriptor != null && descriptor.isResolved() && resourceSpec.getCodecId().equals(descriptor.getCodecId())) {
                    hints.appendTag(descriptor.write());
                    if (hints.tagCount() == QIOStorageConfig.BLOODMAGIC_WILL_TYPE_CAPACITY) {
                        break;
                    }
                }
            }
            if (hints.tagCount() == 0) {
                removeWillHints(stack);
                return;
            }
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            stack.getTagCompound().setTag(WILL_RESOURCES_TAG, hints);
        } catch (LinkageError | RuntimeException ignored) {
            removeWillHints(stack);
        }
    }

    @Nonnull
    private QIODriveRecord getOwnedRecord(ItemStack stack) {
        UUID driveId = getDriveId(stack);
        QIODriveRecord record = QIODriveStorage.INSTANCE.get(driveId);
        if (record == null) {
            throw new IllegalStateException("QIO drive record is unavailable");
        }
        if (!specialization.getRegistryName().equals(record.getSpecializationName())) {
            throw new IllegalStateException("QIO drive record specialization does not match");
        }
        return record;
    }

    /**
     * Keep the deprecated core tier view useful for integrations that use a custom one-type
     * definition. The persisted definition remains the precise custom definition.
     */
    @Override
    public QIODriveTier getDriveTier() {
        switch (tier) {
            case BASE:
                return QIODriveTier.BASE;
            case HYPER_DENSE:
                return QIODriveTier.HYPER_DENSE;
            case TIME_DILATING:
                return QIODriveTier.TIME_DILATING;
            case SUPERMASSIVE:
                return QIODriveTier.SUPERMASSIVE;
            default:
                throw new AssertionError("Unknown QIO storage tier: " + tier);
        }
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, World world, @Nonnull List<String> tooltip,
          @Nonnull ITooltipFlag flag) {
        DriveMetadata metadata = getDriveMetadata(stack);
        BigInteger capacity = tier.getResourceCapacity(resourceSpec);
        tooltip.add(MekanismLang.QIO_DRIVE_TYPE_DETAIL.translateColored(EnumColor.GREY,
              EnumColor.INDIGO, LangUtils.localize(specialization.getTranslationKey()))
              .getFormattedText());
        tooltip.add(MekanismLang.QIO_RESOURCES_DETAIL.translateColored(EnumColor.GREY,
              EnumColor.INDIGO, TextUtils.format(metadata.getStorageUnits()),
              hasUnlimitedCountCapacity(stack) ? LangUtils.localize("gui.infinite") :
                    TextUtils.format(capacity)).getFormattedText());
        tooltip.add(MekanismLang.QIO_TYPES_DETAIL.translateColored(EnumColor.GREY,
              EnumColor.INDIGO, TextUtils.format(metadata.getTypes()),
              hasUnlimitedTypeCapacity(stack) ? LangUtils.localize("gui.infinite") :
                    TextUtils.format(getTypeCapacity(stack))).getFormattedText());
        if (resourceSpec == QIOStorageResourceSpecs.BLOODMAGIC_WILL) {
            tooltip.add(TextFormatting.DARK_GRAY + LangUtils.localize("qio.mekceuqiostorage.tooltip.unit.bloodmagic_will"));
        }
    }

    private static void writeEssentiaHints(ItemStack stack, List<String> tags) {
        removeEssentiaHints(stack);
        if (tags.isEmpty()) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagList entries = new NBTTagList();
        for (String tag : tags) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("key", tag);
            entries.appendTag(entry);
        }
        stack.getTagCompound().setTag(ESSENTIA_TYPES_TAG, entries);
        if (tags.size() == 1) {
            stack.getTagCompound().setString(SINGLE_ESSENTIA_TAG, tags.get(0));
        }
    }

    private static void removeEssentiaHints(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(SINGLE_ESSENTIA_TAG);
            stack.getTagCompound().removeTag(ESSENTIA_TYPES_TAG);
        }
    }

    private static void removeSoulNetworkOwnerHint(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(SOUL_NETWORK_OWNER_TAG + "Most");
            stack.getTagCompound().removeTag(SOUL_NETWORK_OWNER_TAG + "Least");
            stack.getTagCompound().removeTag(SOUL_NETWORK_OWNER_NAME_TAG);
        }
    }

    private static void removeWillHints(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(WILL_RESOURCES_TAG);
        }
    }
}
