package NokhXyr.NokhFrame;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * "World" render mode of the studio. Shaderpacks (Iris/Oculus) never process GUI rendering, so instead of drawing
 * the player in the screen, the real game camera orbits the player and the world render shows through a
 * transparent stage. The player is posed for the whole frame and the 3D scene is drawn in the level at its feet.
 */
public final class StudioWorldView {
    private static StudioAvatarRenderer.@Nullable PoseSnapshot snapshot;
    private static boolean scaledPlayer;

    private StudioWorldView() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, StudioWorldView::beginFrame);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, StudioWorldView::endFrame);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, StudioWorldView::beginPlayer);
        NeoForge.EVENT_BUS.addListener(StudioWorldView::endPlayer);
        NeoForge.EVENT_BUS.addListener(StudioWorldView::renderScene);
        NeoForge.EVENT_BUS.addListener(StudioWorldView::hideHud);
        NeoForge.EVENT_BUS.addListener(StudioWorldView::hideBlockOutline);
    }

    static @Nullable StudioScreen activeStudio() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen instanceof StudioScreen studio && studio.worldView() && minecraft.player != null
                ? studio : null;
    }

    public record Orbit(float yaw, float pitch, float roll, Vec3 target, float distance, float rightShift) {
    }

    /** Camera placement for the current frame, or null when the studio world view is not active. */
    public static @Nullable Orbit orbit(Entity entity, float partialTick) {
        StudioScreen studio = activeStudio();
        if (studio == null || entity != Minecraft.getInstance().player) return null;
        LocalPlayer player = (LocalPlayer) entity;
        // Facing the player's front, then turned by the studio drag angles (same directions as the studio view).
        float yaw = player.yBodyRot + 180.0F - studio.viewYaw();
        float pitch = Mth.clamp(-studio.viewPitch(), -89.9F, 89.9F);
        Vec3 target = player.getPosition(partialTick)
                .add(0.0, player.getBbHeight() / 2.0F + 0.0625F * player.getScale(), 0.0);
        float distance = StudioRules.worldCameraDistance(studio.viewZoom());
        // Keep the player centered in the visible stage rather than the full window (the panel covers the right).
        Minecraft minecraft = Minecraft.getInstance();
        double aspect = (double) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        double halfVertical = Math.toRadians(minecraft.options.fov().get()) / 2.0;
        double visibleWidth = 2.0 * distance * Math.tan(halfVertical) * aspect;
        float rightShift = (float) ((1.0 - studio.stageFraction()) / 2.0 * visibleWidth);
        return new Orbit(yaw, pitch, studio.viewRoll(), target, distance, rightShift);
    }

    private static void beginFrame(RenderFrameEvent.Pre event) {
        StudioScreen studio = activeStudio();
        if (studio == null || snapshot != null) return;
        LocalPlayer player = Minecraft.getInstance().player;
        studio.beginSkinOverride();
        snapshot = StudioAvatarRenderer.PoseSnapshot.apply(player, studio.previewMotion(), studio.motionElapsed(),
                player.yBodyRot);
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
        event.getPoseStack().pushPose();
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
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - minecraft.player.yBodyRot));
            scene.render(pose, buffers);
            buffers.endBatch();
        } finally {
            pose.popPose();
        }
    }

    private static void hideHud(RenderGuiEvent.Pre event) {
        if (activeStudio() != null) event.setCanceled(true);
    }

    private static void hideBlockOutline(RenderHighlightEvent.Block event) {
        if (activeStudio() != null) event.setCanceled(true);
    }
}
