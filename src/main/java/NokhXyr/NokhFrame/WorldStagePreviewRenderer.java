package NokhXyr.NokhFrame;

import NokhXyr.NokhFrame.mixin.CameraAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Replays standard NeoForge world render stages inside the isolated player preview. */
public final class WorldStagePreviewRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldStagePreviewRenderer.class);
    private static final RenderLevelStageEvent.Stage[] BEFORE_PARTICLES = {
            RenderLevelStageEvent.Stage.AFTER_ENTITIES,
            RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
    };
    private static final RenderLevelStageEvent.Stage[] AFTER_PARTICLES = {
            RenderLevelStageEvent.Stage.AFTER_PARTICLES
    };
    private static boolean rendering;
    private static boolean failed;

    private WorldStagePreviewRenderer() {
    }

    public static void renderBeforeParticles(GuiGraphics graphics, LocalPlayer player, Quaternionf sceneRotation) {
        render(graphics, player, sceneRotation, BEFORE_PARTICLES);
    }

    public static void renderAfterParticles(GuiGraphics graphics, LocalPlayer player, Quaternionf sceneRotation) {
        render(graphics, player, sceneRotation, AFTER_PARTICLES);
    }

    private static void render(GuiGraphics graphics, LocalPlayer player, Quaternionf sceneRotation,
                               RenderLevelStageEvent.Stage[] stages) {
        Minecraft minecraft = Minecraft.getInstance();
        if (rendering || failed || minecraft.level == null) return;

        Camera mainCamera = minecraft.gameRenderer.getMainCamera();
        CameraAccessor cameraAccess = (CameraAccessor) mainCamera;
        Vec3 oldPosition = mainCamera.getPosition();
        float oldYaw = mainCamera.getYRot();
        float oldPitch = mainCamera.getXRot();
        float oldRoll = mainCamera.getRoll();
        Vec3 origin = player.position();
        AABB bounds = new AABB(origin.x - 4, origin.y - 3, origin.z - 4,
                origin.x + 4, origin.y + 5, origin.z + 4);
        Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f()) {
            @Override
            public boolean isVisible(AABB box) {
                return box.intersects(bounds);
            }
        };
        frustum.prepare(origin.x, origin.y, origin.z);

        rendering = true;
        try {
            cameraAccess.nokhframe$setPosition(origin);
            Quaternionf orientation = new Quaternionf(sceneRotation).conjugate().rotateY((float) Math.PI);
            Vector3f angles = orientation.getEulerAnglesYXZ(new Vector3f());
            cameraAccess.nokhframe$setRotation(180.0F - (float) Math.toDegrees(angles.y),
                    -(float) Math.toDegrees(angles.x), -(float) Math.toDegrees(angles.z));

            Matrix4f modelView = new Matrix4f(graphics.pose().last().pose());
            Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            for (RenderLevelStageEvent.Stage stage : stages) {
                PoseStack stagePose = new PoseStack();
                stagePose.mulPose(modelView);
                ClientHooks.dispatchRenderStage(stage, minecraft.levelRenderer, stagePose,
                        modelView, projection, minecraft.levelRenderer.getTicks(), mainCamera, frustum);
                graphics.flush();
            }
        } catch (RuntimeException | LinkageError exception) {
            failed = true;
            LOGGER.warn("A world render stage cannot run in the studio preview", exception);
        } finally {
            cameraAccess.nokhframe$setRotation(oldYaw, oldPitch, oldRoll);
            cameraAccess.nokhframe$setPosition(oldPosition);
            rendering = false;
        }
    }
}
