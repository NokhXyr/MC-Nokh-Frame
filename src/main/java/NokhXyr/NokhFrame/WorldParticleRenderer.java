package NokhXyr.NokhFrame;

import NokhXyr.NokhFrame.mixin.ParticleEngineAccessor;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Queue;

/** Draws nearby particles from Minecraft's particle engine in the player preview. */
public final class WorldParticleRenderer {
    private WorldParticleRenderer() {
    }

    public static void render(GuiGraphics graphics, LocalPlayer player, Quaternionf sceneRotation) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.particleEngine instanceof ParticleEngineAccessor access)) return;
        Map<ParticleRenderType, Queue<Particle>> particles = access.nokhframe$getParticles();
        if (particles.isEmpty()) return;

        PreviewCamera camera = new PreviewCamera(player.position(),
                new Quaternionf(sceneRotation).conjugate().rotateY((float) Math.PI));
        Matrix4f transform = new Matrix4f(graphics.pose().last().pose());
        int rendered = 0;
        for (Map.Entry<ParticleRenderType, Queue<Particle>> entry : particles.entrySet()) {
            ParticleRenderType type = entry.getKey();
            if (type != ParticleRenderType.TERRAIN_SHEET
                    && type != ParticleRenderType.PARTICLE_SHEET_OPAQUE
                    && type != ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                    && type != ParticleRenderType.PARTICLE_SHEET_LIT) continue;
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
                    // A particle supplied by another mod may need a world-only render path.
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
