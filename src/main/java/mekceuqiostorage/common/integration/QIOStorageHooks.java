package mekceuqiostorage.common.integration;

import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;
import java.util.Objects;

/** Actual Forge mod ids and the single common-side availability check used by integrations. */
public final class QIOStorageHooks {

    public static final String THAUMCRAFT = "thaumcraft";
    public static final String BOTANIA = "botania";
    public static final String BLOODMAGIC = "bloodmagic";
    public static final String ASTRALSORCERY = "astralsorcery";
    public static final String EMBERS = "embers";
    public static final String NATURESAURA = "naturesaura";
    public static final String PNEUMATICCRAFT = "pneumaticcraft";

    private QIOStorageHooks() {
    }

    public static boolean isLoaded(@Nonnull String modId) {
        Objects.requireNonNull(modId, "mod id");
        try {
            return Loader.isModLoaded(modId);
        } catch (RuntimeException ignored) {
            // Plain unit tests do not run Forge's discovery lifecycle.
            return false;
        }
    }
}
