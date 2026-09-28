package NokhXyr.NokhFrame;

import NokhXyr.NokhFrame.mixin.ParticleEngineAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/** Optional integration with Arcadia Prestige 1.3.x; Nokh Frame has no runtime dependency on it. */
public final class PrestigeIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrestigeIntegration.class);
    private static boolean initialized;
    private static boolean failed;
    private static Method activeEffects;
    private static Method previewEffect;
    private static Method renderCompanion;
    private static Field pieceBudget;

    private PrestigeIntegration() {
    }

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded("arcadia_prestige")) return;
        try {
            Class<?> cache = Class.forName("com.arcadia.prestige.client.PlayerEffectCache");
            Class<?> preview = Class.forName("com.arcadia.prestige.client.EffectPreviewHandler");
            Class<?> blocks = Class.forName("com.arcadia.prestige.client.CosmeticBlockRenderer");
            activeEffects = cache.getMethod("getAll");
            previewEffect = preview.getMethod("previewEffectFor", UUID.class);
            renderCompanion = blocks.getDeclaredMethod("renderFor", Minecraft.class, ClientLevel.class,
                    Vec3.class, PoseStack.class, MultiBufferSource.BufferSource.class, float.class,
                    Player.class, String.class, double.class);
            renderCompanion.setAccessible(true);
            pieceBudget = blocks.getDeclaredField("piecesLeft");
            pieceBudget.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            failed = true;
            LOGGER.warn("Arcadia Prestige preview integration is unavailable", exception);
        }
    }

    public static void render(GuiGraphics graphics, LocalPlayer player, Quaternionf sceneRotation) {
        initialize();
        if (failed || activeEffects == null || previewEffect == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        try {
            String effect = (String) previewEffect.invoke(null, player.getUUID());
            if (effect == null) {
                Object cache = activeEffects.invoke(null);
                if (cache instanceof Map<?, ?> effects) effect = (String) effects.get(player.getUUID());
            }
            if (effect == null || effect.isBlank()) return;
            if (renderCompanion != null && pieceBudget != null) {
                pieceBudget.setInt(null, 96);
                renderCompanion.invoke(null, minecraft, minecraft.level, player.position(), graphics.pose(),
                        graphics.bufferSource(), 1.0F, player, effect, Double.MAX_VALUE);
                graphics.flush();
            }
            renderParticles(graphics, minecraft, player, sceneRotation);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            failed = true;
            LOGGER.warn("Arcadia Prestige preview rendering failed", exception);
        }
    }

    private static void renderParticles(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player,
                                        Quaternionf sceneRotation) {
        if (!(minecraft.particleEngine instanceof ParticleEngineAccessor access)) return;
        Map<ParticleRenderType, Queue<Particle>> particles = access.nokhframe$getParticles();
        if (particles.isEmpty()) return;
        PreviewCamera camera = new PreviewCamera(player.position(),
                new Quaternionf(sceneRotation).conjugate().rotateY((float) Math.PI));
        Matrix4f transform = new Matrix4f(graphics.pose().last().pose());
        int rendered = 0;
        for (Map.Entry<ParticleRenderType, Queue<Particle>> entry : particles.entrySet()) {
            ParticleRenderType type = entry.getKey();
            if (type == ParticleRenderType.NO_RENDER || type == ParticleRenderType.CUSTOM) continue;
            var texture = type == ParticleRenderType.TERRAIN_SHEET
                    ? TextureAtlas.LOCATION_BLOCKS : TextureAtlas.LOCATION_PARTICLES;
            VertexConsumer target = graphics.bufferSource().getBuffer(RenderType.entityTranslucent(texture));
            VertexConsumer transformed = new TransformingConsumer(target, transform);
            for (Particle particle : entry.getValue()) {
                if (rendered >= 400) break;
                if (!particle.isAlive() || particle.getPos().distanceToSqr(player.position()) > 16.0) continue;
                try {
                    particle.render(transformed, camera, 1.0F);
                    rendered++;
                } catch (RuntimeException ignored) {
                    // Some other mods' particles use custom vertex formats or world-only render paths.
                }
            }
            if (rendered >= 400) break;
        }
        graphics.flush();
    }

    private static final class PreviewCamera extends Camera {
        PreviewCamera(Vec3 position, Quaternionf orientation) {
            setPosition(position);
            rotation().set(orientation);
        }
    }

    private static final class TransformingConsumer implements VertexConsumer {
        private final VertexConsumer target;
        private final Matrix4f transform;
        private final Vector3f position = new Vector3f();

        TransformingConsumer(VertexConsumer target, Matrix4f transform) {
            this.target = target;
            this.transform = transform;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            transform.transformPosition(x, y, z, position);
            target.addVertex(position.x, position.y, position.z)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0.0F, 0.0F, 1.0F);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            target.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            target.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            target.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            target.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            target.setNormal(x, y, z);
            return this;
        }
    }
}
