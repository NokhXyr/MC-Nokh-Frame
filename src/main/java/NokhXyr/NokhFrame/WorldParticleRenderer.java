package NokhXyr.NokhFrame;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reuses the particle engine so registered mod particle render types retain their native rendering. */
public final class WorldParticleRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldParticleRenderer.class);
    private static boolean failed;

    private WorldParticleRenderer() {
    }

    public static void render(GuiGraphics graphics, LocalPlayer player, Quaternionf sceneRotation) {
        render(graphics, player.position(), sceneRotation, 4.0);
    }

    public static void render(GuiGraphics graphics, Vec3 origin, Quaternionf sceneRotation, double radius) {
        Minecraft minecraft = Minecraft.getInstance();
        if (failed || minecraft.level == null) return;

        Quaternionf orientation = new Quaternionf(sceneRotation).conjugate().rotateY((float) Math.PI);
        Camera camera = new PreviewCamera(origin, orientation);
        AABB bounds = new AABB(origin.x - radius, origin.y - radius, origin.z - radius,
                origin.x + radius, origin.y + radius, origin.z + radius);
        Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f()) {
            @Override
            public boolean isVisible(AABB box) {
                return box.intersects(bounds);
            }
        };
        frustum.prepare(origin.x, origin.y, origin.z);

        graphics.flush();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.mul(graphics.pose().last().pose());
            RenderSystem.applyModelViewMatrix();
            minecraft.particleEngine.render(minecraft.gameRenderer.lightTexture(), camera, 1.0F,
                    frustum, type -> true);
        } catch (RuntimeException | LinkageError exception) {
            failed = true;
            LOGGER.warn("Native particles cannot be rendered in the studio preview", exception);
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static final class PreviewCamera extends Camera {
        PreviewCamera(Vec3 position, Quaternionf orientation) {
            setPosition(position);
            Vector3f angles = orientation.getEulerAnglesYXZ(new Vector3f());
            setRotation(180.0F - (float) Math.toDegrees(angles.y),
                    -(float) Math.toDegrees(angles.x), -(float) Math.toDegrees(angles.z));
        }
    }
}
