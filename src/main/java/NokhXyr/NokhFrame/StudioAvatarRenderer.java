package NokhXyr.NokhFrame;

import NokhXyr.NokhFrame.mixin.WalkAnimationStateAccessor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/** Renders the real player temporarily posed for the studio, then restores every changed field. */
public final class StudioAvatarRenderer {
    public enum Motion { IDLE, WALK, RUN, SNEAK, ATTACK }

    private static boolean renderingMotion;
    private static boolean posingModel;
    private static boolean crouching;
    private static Motion currentMotion = Motion.IDLE;
    private static float motionSeconds;
    private static @Nullable StudioPose activePose;

    private StudioAvatarRenderer() {
    }

    public static void render(GuiGraphics graphics, LocalPlayer player, int left, int top, int right, int bottom,
                              int scale, float yaw, float pitch, float roll, Motion motion, float elapsedSeconds,
                              @Nullable StudioScene scene, int stageRight, int stageBottom, float playerSize,
                              StudioPlacement placement, StudioPose studioPose) {
        float entityScale = player.getScale();
        float stageScale = scale / entityScale;
        float centerX = (left + right) / 2.0F + placement.panX * stageScale;
        float centerY = (top + bottom) / 2.0F + placement.panY * stageScale;
        Vector3f translate = new Vector3f(0.0F, player.getBbHeight() / 2.0F + 0.0625F * entityScale, 0.0F);
        Quaternionf camera = new Quaternionf()
                .rotateY((float) Math.toRadians(yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
        if (scene != null) {
            // The set fills the whole stage, not just the player's margins.
            graphics.flush();
            graphics.enableScissor(0, 0, stageRight, stageBottom);
            graphics.pose().pushPose();
            try {
                applyStageTransform(graphics, centerX, centerY, stageScale, translate, pose);
                // The view follows the player: the scene shifts opposite to the player's studio offset.
                Vector3f standing = scenePoint(placement.playerX, placement.playerY, placement.playerZ);
                graphics.pose().translate(-standing.x, -standing.y, -standing.z);
                applyScenePlacement(graphics.pose(), placement);
                scene.renderCached(graphics.pose());
            } finally {
                Lighting.setupFor3DItems();
                graphics.pose().popPose();
                graphics.disableScissor();
            }
        }

        // A bigger player keeps its feet on the same spot: the pre-scale offset shrinks as the scale grows.
        float playerScale = stageScale * playerSize;
        Vector3f playerTranslate = new Vector3f(translate).div(playerSize);
        // A moved player may leave the default margins, so it is clipped to the whole stage like the scene.
        graphics.enableScissor(0, 0, stageRight, stageBottom);
        PoseSnapshot snapshot = PoseSnapshot.apply(player, motion, elapsedSeconds, 180.0F + placement.playerRotation, studioPose);
        posingModel = true;
        try {
            InventoryScreen.renderEntityInInventory(graphics, centerX, centerY,
                    playerScale, playerTranslate, pose, camera, player);
            graphics.pose().pushPose();
            try {
                applyStageTransform(graphics, centerX, centerY, playerScale, playerTranslate, pose);
                OwnedPetPreviewRenderer.render(graphics, player);
                WorldStagePreviewRenderer.renderBeforeParticles(graphics, player, camera);
                WorldParticleRenderer.render(graphics, player, camera);
                WorldStagePreviewRenderer.renderAfterParticles(graphics, player, camera);
            } finally {
                graphics.pose().popPose();
            }
        } finally {
            posingModel = false;
            snapshot.restore();
            graphics.disableScissor();
        }
    }

    /** Same transform as {@link InventoryScreen#renderEntityInInventory}: the origin becomes the player's feet. */
    private static void applyStageTransform(GuiGraphics graphics, float centerX, float centerY, float scale,
                                            Vector3f translate, Quaternionf pose) {
        graphics.pose().translate(centerX, centerY, 50.0F);
        graphics.pose().scale(scale, scale, -scale);
        graphics.pose().translate(translate.x, translate.y, translate.z);
        graphics.pose().mulPose(pose);
    }

    /**
     * Studio axes to scene coordinates: studio X points right from the default front view, which is the scene's
     * negative X (the viewer looks at the player's face, towards +Z).
     */
    static Vector3f scenePoint(float x, float y, float z) {
        return new Vector3f(-x, y, z);
    }

    /** Moves, turns and scales the scene from the player's feet. Shared by the studio and world views. */
    static void applyScenePlacement(PoseStack pose, StudioPlacement placement) {
        Vector3f offset = scenePoint(placement.sceneX, placement.sceneY, placement.sceneZ);
        pose.translate(offset.x, offset.y, offset.z);
        pose.mulPose(Axis.YP.rotationDegrees(-placement.sceneRotation));
        pose.scale(placement.sceneScale, placement.sceneScale, placement.sceneScale);
    }

    /** Model posing for the local player's own draw call (walk, run and sneak limbs). */
    static void setPosingModel(boolean posing) {
        posingModel = posing;
    }

    public static boolean isPosingModel() {
        return posingModel && renderingMotion;
    }

    public static @Nullable StudioPose activePose() {
        return activePose;
    }

    public static boolean isRenderingMotion() {
        return renderingMotion;
    }

    public static boolean isCrouching() {
        return crouching;
    }

    public static boolean isRunning() {
        return currentMotion == Motion.RUN;
    }

    public static Motion currentMotion() {
        return currentMotion;
    }

    public static float motionSeconds() {
        return motionSeconds;
    }

    public static Vec3 simulatedDelta() {
        return simulatedDelta(currentMotion);
    }

    public static Vec3 simulatedDelta(Motion motion) {
        return switch (motion) {
            case WALK -> new Vec3(0.0, 0.0, -0.12);
            case RUN -> new Vec3(0.0, 0.0, -0.26);
            case SNEAK -> new Vec3(0.0, 0.0, -0.06);
            default -> Vec3.ZERO;
        };
    }

    /** Studio pose applied to the real player object; {@link #restore()} puts every changed field back. */
    static final class PoseSnapshot {
        private final LocalPlayer player;
        private final float body, bodyPrevious, yaw, yawPrevious, head, headPrevious, pitch, pitchPrevious;
        private final double xPrevious, yPrevious, zPrevious;
        private final float attack, attackPrevious;
        private final @Nullable InteractionHand swingingArm;
        private final float walkSpeed, walkSpeedPrevious, walkPosition;

        private PoseSnapshot(LocalPlayer player) {
            this.player = player;
            body = player.yBodyRot;
            bodyPrevious = player.yBodyRotO;
            yaw = player.getYRot();
            yawPrevious = player.yRotO;
            head = player.yHeadRot;
            headPrevious = player.yHeadRotO;
            pitch = player.getXRot();
            pitchPrevious = player.xRotO;
            xPrevious = player.xOld;
            yPrevious = player.yOld;
            zPrevious = player.zOld;
            attack = player.attackAnim;
            attackPrevious = player.oAttackAnim;
            swingingArm = player.swingingArm;
            WalkAnimationState walk = player.walkAnimation;
            walkSpeed = walk.speed();
            walkSpeedPrevious = ((WalkAnimationStateAccessor) walk).nokhframe$getSpeedOld();
            walkPosition = walk.position();
        }

        static PoseSnapshot apply(LocalPlayer player, Motion motion, float elapsedSeconds, float facingYaw,
                                  @Nullable StudioPose pose) {
            PoseSnapshot snapshot = new PoseSnapshot(player);
            renderingMotion = true;
            activePose = pose;
            currentMotion = motion;
            motionSeconds = elapsedSeconds;
            crouching = motion == Motion.SNEAK;
            player.yBodyRot = facingYaw;
            player.yBodyRotO = facingYaw;
            player.setYRot(facingYaw);
            player.yRotO = facingYaw;
            player.yHeadRot = facingYaw;
            player.yHeadRotO = facingYaw;
            player.setXRot(0.0F);
            player.xRotO = 0.0F;
            player.xOld = player.getX();
            player.yOld = player.getY();
            player.zOld = player.getZ();
            float speed = switch (motion) {
                case WALK -> 0.6F;
                case RUN -> 1.0F;
                case SNEAK -> 0.35F;
                default -> 0.0F;
            };
            WalkAnimationState walk = player.walkAnimation;
            WalkAnimationStateAccessor access = (WalkAnimationStateAccessor) walk;
            walk.setSpeed(speed);
            access.nokhframe$setSpeedOld(speed);
            access.nokhframe$setPosition(elapsedSeconds * 20.0F * speed);
            float attack = motion == Motion.ATTACK ? StudioRules.attackProgress(elapsedSeconds) : 0.0F;
            player.attackAnim = attack;
            player.oAttackAnim = attack;
            // Vanilla animates the off hand while swingingArm is null (no swing yet since joining).
            player.swingingArm = InteractionHand.MAIN_HAND;
            return snapshot;
        }

        void restore() {
            renderingMotion = false;
            activePose = null;
            currentMotion = Motion.IDLE;
            motionSeconds = 0.0F;
            crouching = false;
            player.yBodyRot = body;
            player.yBodyRotO = bodyPrevious;
            player.setYRot(yaw);
            player.yRotO = yawPrevious;
            player.yHeadRot = head;
            player.yHeadRotO = headPrevious;
            player.setXRot(pitch);
            player.xRotO = pitchPrevious;
            player.xOld = xPrevious;
            player.yOld = yPrevious;
            player.zOld = zPrevious;
            player.attackAnim = attack;
            player.oAttackAnim = attackPrevious;
            player.swingingArm = swingingArm;
            WalkAnimationState walk = player.walkAnimation;
            WalkAnimationStateAccessor access = (WalkAnimationStateAccessor) walk;
            walk.setSpeed(walkSpeed);
            access.nokhframe$setSpeedOld(walkSpeedPrevious);
            access.nokhframe$setPosition(walkPosition);
        }
    }
}
