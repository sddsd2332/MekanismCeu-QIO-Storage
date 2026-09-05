package mekceuqiostorage.client.integration.pneumaticcraft;

import mekceuqiostorage.client.AbstractQIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Renders PneumaticCraft air with its native animated air-particle texture. */
@SideOnly(Side.CLIENT)
public final class PneumaticCraftAirRenderer
        extends AbstractQIOResourceRenderer<QIOStorageResources.Scalar> {

    private static final ResourceLocation AIR_PARTICLE_TEXTURE =
            new ResourceLocation("pneumaticcraft", "textures/particle/air_particle.png");
    private static final int SLOT_SIZE = 16;
    private static final int TEXTURE_SIZE = 32;
    private static final int EMIT_MIN = 3;
    private static final int EMIT_MAX = 10;
    private static final float SPEED = 0.3F;
    private static final int BASE_SIZE = 6;
    private static final int SIZE_JITTER = 1;
    private static final int MAX_LIFE = 50;
    private static final float START_ALPHA = 0.9F;
    private static final Gui GUI = new Gui();

    public static final PneumaticCraftAirRenderer INSTANCE = new PneumaticCraftAirRenderer();

    private final QIOStorageResources.Scalar resource;
    private final String registryName;
    private final Emitter emitter = new Emitter();

    private PneumaticCraftAirRenderer() {
        super(QIOStorageResourceSpecs.PNEUMATICCRAFT_AIR);
        resource = QIOStorageResources.AIR;
        registryName = getResourceSpec().getModId() + ':' + resource;
    }

    @Override
    public void render(@Nonnull QIOStorageResources.Scalar value, int x, int y) {
        requireResource(value);
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            return;
        }

        long gameTime = minecraft.world.getTotalWorldTime();
        float now = gameTime + minecraft.getRenderPartialTicks();
        emitter.tickAndSpawn(minecraft, gameTime);

        minecraft.getTextureManager().bindTexture(AIR_PARTICLE_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        emitter.drawAt(now, x, y);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
    }

    @Override
    @Nonnull
    public String getDisplayName(@Nonnull QIOStorageResources.Scalar value) {
        requireResource(value);
        return I18n.format("qio.mekceuqiostorage.resource.pneumaticcraft_air");
    }

    @Override
    @Nonnull
    public String getRegistryName(@Nonnull QIOStorageResources.Scalar value) {
        requireResource(value);
        return registryName;
    }

    @Override
    @Nonnull
    public List<String> getTooltip(@Nonnull QIOStorageResources.Scalar value) {
        requireResource(value);
        return Arrays.asList(getDisplayName(value),
                TextFormatting.DARK_GRAY + I18n.format(
                        "qio.mekceuqiostorage.tooltip.source",
                        I18n.format("qio.mekceuqiostorage.source.pneumaticcraft")),
                TextFormatting.DARK_GRAY + I18n.format(
                        "qio.mekceuqiostorage.tooltip.unit.pneumaticcraft_air"));
    }

    private void requireResource(QIOStorageResources.Scalar value) {
        if (!getResourceSpec().getCodec().sameType(resource, value)) {
            throw new IllegalArgumentException("Renderer received the wrong QIO resource type");
        }
    }

    private static final class Emitter {
        private final List<Particle> particles = new ArrayList<>();
        private long lastEmitTick = Long.MIN_VALUE;

        void tickAndSpawn(Minecraft minecraft, long gameTime) {
            particles.removeIf(p -> gameTime - p.birthTick > MAX_LIFE);
            if (lastEmitTick == gameTime) {
                return;
            }
            lastEmitTick = gameTime;

            for (int i = 0; i < MathHelper.getInt(minecraft.world.rand, EMIT_MIN, EMIT_MAX); i++) {
                float angle = minecraft.world.rand.nextFloat() * (float) (Math.PI * 2D);
                float dirX = MathHelper.cos(angle);
                float dirY = MathHelper.sin(angle);
                int size = MathHelper.clamp(BASE_SIZE +
                        MathHelper.getInt(minecraft.world.rand, -SIZE_JITTER, SIZE_JITTER), 1, SLOT_SIZE);
                float x0 = SLOT_SIZE / 2F - size / 2F;
                float y0 = SLOT_SIZE / 2F - size / 2F;
                float speed = SPEED * (0.9F + 0.2F * minecraft.world.rand.nextFloat());
                float vx = dirX * speed;
                float vy = dirY * speed;
                float right = SLOT_SIZE - size;
                float bottom = SLOT_SIZE - size;
                float timeX = vx > 0F ? (right - x0) / vx : vx < 0F ? (0F - x0) / vx : Float.POSITIVE_INFINITY;
                float timeY = vy > 0F ? (bottom - y0) / vy : vy < 0F ? (0F - y0) / vy : Float.POSITIVE_INFINITY;
                float life = Math.min(Math.min(positive(timeX), positive(timeY)), MAX_LIFE);
                if (!Float.isFinite(life)) {
                    life = 1F;
                }
                particles.add(new Particle(x0, y0, vx, vy,
                        gameTime - minecraft.world.rand.nextFloat() * 0.1F, life, size));
            }
        }

        void drawAt(float now, int offsetX, int offsetY) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                float age = Math.max(0F, now - p.birthTick);
                if (age > p.life) {
                    particles.remove(i);
                    continue;
                }
                float x = MathHelper.clamp(p.x0 + p.vx * age, 0F, SLOT_SIZE - p.size);
                float y = MathHelper.clamp(p.y0 + p.vy * age, 0F, SLOT_SIZE - p.size);
                float t = age / p.life;
                float alpha = t < 0.15F ? START_ALPHA * t / 0.15F
                        : START_ALPHA * (1F - (t - 0.15F) / 0.85F);
                GlStateManager.color(1F, 1F, 1F, MathHelper.clamp(alpha, 0F, 1F));
                GUI.drawModalRectWithCustomSizedTexture((int) (offsetX + x), (int) (offsetY + y),
                        0, 0, p.size, p.size, TEXTURE_SIZE, TEXTURE_SIZE);
            }
        }

        private static float positive(float value) {
            return value > 0F && Float.isFinite(value) ? value : Float.POSITIVE_INFINITY;
        }
    }

    private static final class Particle {
        private final float x0, y0, vx, vy, birthTick, life;
        private final int size;

        private Particle(float x0, float y0, float vx, float vy,
                         float birthTick, float life, int size) {
            this.x0 = x0;
            this.y0 = y0;
            this.vx = vx;
            this.vy = vy;
            this.birthTick = birthTick;
            this.life = life;
            this.size = size;
        }
    }
}
