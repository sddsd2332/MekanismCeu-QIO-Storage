package mekceuqiostorage.client.integration.embers;

import mekceuqiostorage.client.AbstractQIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
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

/** Renders Embers with the native star particle used by ember packets. */
@SideOnly(Side.CLIENT)
public final class EmbersEmberRenderer
        extends AbstractQIOResourceRenderer<QIOStorageResources.Scalar> {

    public static final EmbersEmberRenderer INSTANCE = new EmbersEmberRenderer();

    private static final ResourceLocation STAR_SPRITE_ID =
            new ResourceLocation("embers:entity/particle_star");
    private static final int SLOT_SIZE = 16;
    private static final int EMIT_MIN = 3;
    private static final int EMIT_MAX = 7;
    private static final int BASE_SIZE = 12;
    private static final int SIZE_JITTER = 2;
    private static final int MAX_LIFE = 40;
    private static final float START_ALPHA = 1F;
    private static final Gui GUI = new Gui();

    private final QIOStorageResources.Scalar resource;
    private final String registryName;
    private final Emitter emitter = new Emitter();

    private EmbersEmberRenderer() {
        super(QIOStorageResourceSpecs.EMBERS_EMBER);
        resource = QIOStorageResources.EMBER;
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
        emitter.tickAndSpawn(minecraft, gameTime);

        TextureAtlasSprite star = minecraft.getTextureMapBlocks().getAtlasSprite(STAR_SPRITE_ID.toString());
        if (star == null) {
            return;
        }
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        emitter.drawAt(star, gameTime + minecraft.getRenderPartialTicks(), x, y);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
    }

    @Override
    @Nonnull
    public String getDisplayName(@Nonnull QIOStorageResources.Scalar value) {
        requireResource(value);
        return I18n.format("qio.mekceuqiostorage.resource.embers_ember");
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
                        I18n.format("qio.mekceuqiostorage.source.embers")),
                TextFormatting.DARK_GRAY + I18n.format(
                        "qio.mekceuqiostorage.tooltip.unit.embers_ember"));
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
            int count = MathHelper.getInt(minecraft.world.rand, EMIT_MIN, EMIT_MAX);
            for (int i = 0; i < count; i++) {
                float angle = minecraft.world.rand.nextFloat() * (float) (Math.PI * 2D);
                float speed = 0.08F + 0.06F * minecraft.world.rand.nextFloat();
                int size = MathHelper.clamp(BASE_SIZE +
                        MathHelper.getInt(minecraft.world.rand, -SIZE_JITTER, SIZE_JITTER), 2, SLOT_SIZE);
                float life = 20F + 20F * minecraft.world.rand.nextFloat();
                particles.add(new Particle(SLOT_SIZE / 2F, SLOT_SIZE / 2F,
                        MathHelper.cos(angle) * speed, MathHelper.sin(angle) * speed,
                        gameTime - minecraft.world.rand.nextFloat() * 0.1F, life, size,
                        minecraft.world.rand.nextFloat() * 360F));
            }
        }

        void drawAt(TextureAtlasSprite star, float now, int offsetX, int offsetY) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                float age = Math.max(0F, now - p.birthTick);
                if (age > p.life) {
                    particles.remove(i);
                    continue;
                }
                float progress = MathHelper.clamp(age / p.life, 0F, 1F);
                float scale = p.size * (1F - progress);
                float alpha = START_ALPHA * (1F - progress) * (1F - progress);
                float centerX = offsetX + p.x0 + p.vx * age;
                float centerY = offsetY + p.y0 + p.vy * age;
                float px = centerX - scale / 2F;
                float py = centerY - scale / 2F;
                px = MathHelper.clamp(px, offsetX, offsetX + SLOT_SIZE - scale);
                py = MathHelper.clamp(py, offsetY, offsetY + SLOT_SIZE - scale);

                GlStateManager.pushMatrix();
                GlStateManager.translate(px + scale / 2F, py + scale / 2F, 0F);
                GlStateManager.rotate(p.rotation + age * 3F, 0F, 0F, 1F);
                GlStateManager.translate(-scale / 2F, -scale / 2F, 0F);
                GlStateManager.color(1F, 64F / 255F, 16F / 255F, MathHelper.clamp(alpha, 0F, 1F));
                GUI.drawTexturedModalRect(0, 0, star, Math.max(1, (int) scale), Math.max(1, (int) scale));
                GlStateManager.popMatrix();
            }
        }
    }

    private static final class Particle {
        private final float x0, y0, vx, vy, birthTick, life, size, rotation;

        private Particle(float x0, float y0, float vx, float vy,
                         float birthTick, float life, float size, float rotation) {
            this.x0 = x0;
            this.y0 = y0;
            this.vx = vx;
            this.vy = vy;
            this.birthTick = birthTick;
            this.life = life;
            this.size = size;
            this.rotation = rotation;
        }
    }
}
