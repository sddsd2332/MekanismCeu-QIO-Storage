package mekceuqiostorage.common.content.qio;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Immutable, amount-free values used by the addon codecs. */
public final class QIOStorageResources {

    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9_.-]+");
    private static final int MAX_KEY_LENGTH = 128;
    private static final UUID ZERO_UUID = new UUID(0, 0);

    public static final Scalar MANA = Scalar.of("mana");
    public static final Scalar STARLIGHT = Scalar.of("starlight");
    public static final Scalar EMBER = Scalar.of("ember");
    public static final Scalar AURA = Scalar.of("aura");
    public static final Scalar AIR = Scalar.of("air");

    private QIOStorageResources() {
    }

    /** Stable Will identities without linking Blood Magic classes into common persistence. */
    public enum DemonWill {
        DEFAULT("default"),
        CORROSIVE("corrosive"),
        DESTRUCTIVE("destructive"),
        VENGEFUL("vengeful"),
        STEADFAST("steadfast");

        private final String key;

        DemonWill(String key) {
            this.key = key;
        }

        @Nonnull
        public String getKey() {
            return key;
        }

        @Nonnull
        public static DemonWill fromKey(String key) {
            for (DemonWill type : values()) {
                if (type.key.equals(key)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown Demon Will type: " + key);
        }
    }

    /** A fixed-key scalar resource such as mana, LP or air. */
    public static final class Scalar {

        private final String key;

        private Scalar(String key) {
            this.key = requireKey(key);
        }

        @Nonnull
        public static Scalar of(String key) {
            return new Scalar(key);
        }

        @Nonnull
        public String getKey() {
            return key;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof Scalar && key.equals(((Scalar) obj).key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /** One Blood Magic Soul Network, identified independently of its owner's mutable name. */
    public static final class SoulNetworkLP {

        private final UUID ownerId;
        @Nullable
        private final String ownerName;

        private SoulNetworkLP(UUID ownerId, @Nullable String ownerName) {
            this.ownerId = Objects.requireNonNull(ownerId, "Soul Network owner cannot be null");
            if (ZERO_UUID.equals(ownerId)) {
                throw new IllegalArgumentException("Soul Network owner cannot be the zero UUID");
            }
            this.ownerName = normalizeOwnerName(ownerName);
        }

        @Nonnull
        public static SoulNetworkLP of(UUID ownerId) {
            return new SoulNetworkLP(ownerId, null);
        }

        @Nonnull
        public static SoulNetworkLP of(UUID ownerId, @Nullable String ownerName) {
            return new SoulNetworkLP(ownerId, ownerName);
        }

        @Nonnull
        public UUID getOwnerId() {
            return ownerId;
        }

        @Nullable
        public String getOwnerName() {
            return ownerName;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof SoulNetworkLP &&
                  ownerId.equals(((SoulNetworkLP) obj).ownerId);
        }

        @Override
        public int hashCode() {
            return ownerId.hashCode();
        }

        @Override
        public String toString() {
            return ownerId.toString();
        }

        @Nullable
        private static String normalizeOwnerName(@Nullable String ownerName) {
            if (ownerName == null) {
                return null;
            }
            String trimmed = ownerName.trim();
            if (trimmed.isEmpty() || trimmed.length() > 64) {
                return null;
            }
            for (int i = 0; i < trimmed.length(); i++) {
                if (Character.isISOControl(trimmed.charAt(i))) {
                    return null;
                }
            }
            return trimmed;
        }
    }

    /** Stable Thaumcraft Aspect identity, represented without linking the optional API. */
    public static final class Essentia {

        private final String aspectTag;

        private Essentia(String aspectTag) {
            this.aspectTag = requireKey(aspectTag);
        }

        @Nonnull
        public static Essentia of(String aspectTag) {
            return new Essentia(aspectTag);
        }

        @Nonnull
        public String getAspectTag() {
            return aspectTag;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof Essentia && aspectTag.equals(((Essentia) obj).aspectTag);
        }

        @Override
        public int hashCode() {
            return aspectTag.hashCode();
        }

        @Override
        public String toString() {
            return aspectTag;
        }
    }

    @Nonnull
    static String requireKey(String value) {
        Objects.requireNonNull(value, "QIO resource key cannot be null");
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!value.equals(normalized) || value.isEmpty() || value.length() > MAX_KEY_LENGTH ||
              !KEY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid QIO resource key: " + value);
        }
        return value;
    }
}
