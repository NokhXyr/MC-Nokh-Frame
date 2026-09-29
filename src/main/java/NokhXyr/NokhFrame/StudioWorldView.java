package NokhXyr.NokhFrame;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * "World" render mode of the studio. Shaderpacks (Iris/Oculus) never process GUI rendering, so instead of drawing
 * the player in the screen, the real game camera orbits the player and the world render shows through a
 * transparent stage. The player is posed for the whole frame and the 3D scene is drawn in the level at its feet.
 */
public final class StudioWorldView {
    private static StudioAvatarRenderer.@Nullable PoseSnapshot snapshot;
    private static boolean scaledPlayer;
    /** The player's real facing this frame; the studio rotation only turns the rendered model. */
    private static float baseYaw;

    private StudioWorldView() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, StudioWorldView::beginFrame);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, StudioWorldView::endFrame);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, StudioWorldView::beginPlayer);
        NeoForge.EVENT_BUS.addListener(StudioWorldView::endPlayer);
        NeoForge.EVENT_BUS.addListener(StudioWorldView::renderScene);
        NeoForge.EVENT_BUS.addListener(StudioWorldView::renderBackdrop);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, RenderLivingEvent.Pre.class, StudioWorldView::hideOtherEntities);
        NeoForge.EVENT_BUS.addListener(StudioWorldView::hideHud);
        NeoForge.EVENT_BUS.addListener(StudioWorldView::hideBlockOutline);
    }

    static @Nullable StudioScreen activeStudio() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen instanceof StudioScreen studio && studio.worldView() && minecraft.player != null
                ? studio : null;
    }

    public record Orbit(float yaw, float pitch, float roll, Vec3 target, float distance, float rightShift, float up) {
    }

    /** Camera placement for the current frame, or null when the studio world view is not active. */
    public static @Nullable Orbit orbit(Entity entity, float partialTick) {
        StudioScreen studio = activeStudio();
        if (studio == null || entity != Minecraft.getInstance().player) return null;
        LocalPlayer player = (LocalPlayer) entity;
        // Facing the player's front, then turned by the studio drag angles (same directions as the studio view).
        float facing = snapshot != null ? baseYaw : studio.worldFacing(player.getYRot());
        float yaw = facing + 180.0F - studio.viewYaw();
        float pitch = Mth.clamp(-studio.viewPitch(), -89.9F, 89.9F);
        StudioPlacement placement = studio.placement();
        // The camera follows the player's studio offset, like the studio view.
        Vector3f standing = rotationFor(facing).transform(
                StudioAvatarRenderer.scenePoint(placement.playerX, placement.playerY, placement.playerZ));
        Vec3 target = player.getPosition(partialTick)
                .add(standing.x, standing.y + player.getBbHeight() / 2.0F + 0.0625F * player.getScale(), standing.z);
        float distance = StudioRules.worldCameraDistance(studio.viewZoom());
        // Keep the player centered in the visible stage rather than the full window (the panel covers the right).
        Minecraft minecraft = Minecraft.getInstance();
        double aspect = (double) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        double halfVertical = Math.toRadians(minecraft.options.fov().get()) / 2.0;
        double visibleWidth = 2.0 * distance * Math.tan(halfVertical) * aspect;
        float rightShift = (float) ((1.0 - studio.stageFraction()) / 2.0 * visibleWidth);
        return new Orbit(yaw, pitch, studio.viewRoll(), target, distance, rightShift - placement.panX, placement.panY);
    }

    private static void beginFrame(RenderFrameEvent.Pre event) {
        StudioScreen studio = activeStudio();
        if (studio == null || snapshot != null) return;
        LocalPlayer player = Minecraft.getInstance().player;
        studio.beginSkinOverride();
        // Locked when the world view starts: the real body yaw keeps easing toward the head every tick, which made
        // the camera, the scene and the head drift and twitch.
        baseYaw = studio.worldFacing(player.getYRot());
        snapshot = StudioAvatarRenderer.PoseSnapshot.apply(player, studio.previewMotion(), studio.motionElapsed(),
                baseYaw + studio.placement().playerRotation, studio.pose());
    }

    private static void endFrame(RenderFrameEvent.Post event) {
        if (snapshot == null) return;
        snapshot.restore();
        snapshot = null;
        SkinOverride.end();
    }

    private static void beginPlayer(RenderPlayerEvent.Pre event) {
        StudioScreen studio = activeStudio();
        if (studio == null || snapshot == null || event.getEntity() != Minecraft.getInstance().player) return;
        StudioAvatarRenderer.setPosingModel(true);
        float size = studio.playerSize();
        StudioPlacement placement = studio.placement();
        Vector3f offset = facingRotation().transform(
                StudioAvatarRenderer.scenePoint(placement.playerX, placement.playerY, placement.playerZ));
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(offset.x, offset.y, offset.z);
        event.getPoseStack().scale(size, size, size);
        scaledPlayer = true;
    }

    private static void endPlayer(RenderPlayerEvent.Post event) {
        if (!scaledPlayer || event.getEntity() != Minecraft.getInstance().player) return;
        event.getPoseStack().popPose();
        scaledPlayer = false;
        StudioAvatarRenderer.setPosingModel(false);
    }

    private static void renderScene(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        StudioScreen studio = activeStudio();
        StudioScene scene = studio == null ? null : studio.scene();
        PoseStack pose = event.getPoseStack();
        if (scene == null || pose == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 offset = minecraft.player.getPosition(partialTick).subtract(event.getCamera().getPosition());
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        pose.pushPose();
        try {
            // Lifted 1 cm so the scene floor does not z-fight with the ground block under the player.
            pose.translate(offset.x, offset.y + 0.01, offset.z);
            // Scenes are authored with +Z behind the player: turn them to match the player's facing.
            pose.mulPose(facingRotation());
            StudioAvatarRenderer.applyScenePlacement(pose, studio.placement());
            scene.render(pose, buffers);
            buffers.endBatch();
        } finally {
            pose.popPose();
        }
    }

    /** Scene space to world space: scenes are authored with +Z behind the player, turned to the player's facing. */
    private static Quaternionf facingRotation() {
        return rotationFor(baseYaw);
    }

    private static Quaternionf rotationFor(float facing) {
        return Axis.YP.rotationDegrees(180.0F - facing);
    }

    /** Studio view drawn through the world pipeline because a shaderpack is active: the world itself is hidden. */
    public static boolean isolating() {
        StudioScreen studio = activeStudio();
        return studio != null && studio.shaderStudio();
    }

    /** Keeps only the local player and the pets it owns while the studio is isolated from the world. */
    private static void hideOtherEntities(RenderLivingEvent.Pre<?, ?> event) {
        if (!isolating()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        LivingEntity entity = event.getEntity();
        if (entity == player) return;
        if (entity instanceof OwnableEntity owned && player != null && player.getUUID().equals(owned.getOwnerUUID())) return;
        event.setCanceled(true);
    }

    /**
     * Studio backdrop for the isolated view: the chosen color or PNG on a camera-facing plane behind the scene,
     * sized to fill the view and cropped like the studio background.
     */
    private static void renderBackdrop(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !isolating()) return;
        StudioScreen studio = activeStudio();
        PoseStack pose = event.getPoseStack();
        if (studio == null || pose == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vector3f look = new Vector3f(camera.getLookVector());
        Vector3f up = new Vector3f(camera.getUpVector());
        Vector3f right = new Vector3f(look).cross(up).normalize();

        StudioScene scene = studio.scene();
        StudioPlacement placement = studio.placement();
        float sceneReach = scene == null ? 0.0F : scene.mesh().radius() * placement.sceneScale
                + new Vector3f(placement.sceneX, placement.sceneY, placement.sceneZ).length()
                + new Vector3f(placement.playerX, placement.playerY, placement.playerZ).length();
        float distance = StudioRules.worldCameraDistance(studio.viewZoom()) + Math.max(8.0F, sceneReach + 2.0F);
        double aspect = (double) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        float halfHeight = (float) (distance * Math.tan(Math.toRadians(minecraft.options.fov().get()) / 2.0) * 1.25);
        float halfWidth = (float) (halfHeight * aspect);

        ResourceLocation texture = studio.backdropTexture();
        float u0 = 0.0F, u1 = 1.0F, v0 = 0.0F, v1 = 1.0F;
        if (texture != null && studio.backdropWidth() > 0 && studio.backdropHeight() > 0) {
            float planeAspect = halfWidth / halfHeight;
            float imageAspect = (float) studio.backdropWidth() / studio.backdropHeight();
            if (imageAspect > planeAspect) {
                float span = planeAspect / imageAspect;
                u0 = (1.0F - span) / 2.0F;
                u1 = u0 + span;
            } else {
                float span = imageAspect / planeAspect;
                v0 = (1.0F - span) / 2.0F;
                v1 = v0 + span;
            }
        } else {
            texture = whiteTexture();
        }
        int color = texture == whiteTexture() ? studio.backdropColor() : -1;

        Vector3f center = new Vector3f(look).mul(distance);
        Vector3f dx = new Vector3f(right).mul(halfWidth);
        Vector3f dy = new Vector3f(up).mul(halfHeight);
        Vector3f normal = new Vector3f(look).negate();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose last = pose.last();
        backdropVertex(consumer, last, new Vector3f(center).sub(dx).add(dy), color, u0, v0, normal);
        backdropVertex(consumer, last, new Vector3f(center).sub(dx).sub(dy), color, u0, v1, normal);
        backdropVertex(consumer, last, new Vector3f(center).add(dx).sub(dy), color, u1, v1, normal);
        backdropVertex(consumer, last, new Vector3f(center).add(dx).add(dy), color, u1, v0, normal);
        buffers.endBatch();
    }

    private static void backdropVertex(VertexConsumer consumer, PoseStack.Pose pose, Vector3f position, int color,
                                       float u, float v, Vector3f normal) {
        consumer.addVertex(pose, position.x, position.y, position.z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }

    private static @Nullable ResourceLocation whiteTexture;

    private static ResourceLocation whiteTexture() {
        if (whiteTexture == null) {
            NativeImage image = new NativeImage(1, 1, false);
            image.setPixelRGBA(0, 0, -1);
            whiteTexture = ResourceLocation.fromNamespaceAndPath(NokhFrameMod.MOD_ID, "backdrop_white");
            Minecraft.getInstance().getTextureManager().register(whiteTexture, new DynamicTexture(image));
        }
        return whiteTexture;
    }

    private static void hideHud(RenderGuiEvent.Pre event) {
        if (activeStudio() != null) event.setCanceled(true);
    }

    private static void hideBlockOutline(RenderHighlightEvent.Block event) {
        if (activeStudio() != null) event.setCanceled(true);
    }
}
