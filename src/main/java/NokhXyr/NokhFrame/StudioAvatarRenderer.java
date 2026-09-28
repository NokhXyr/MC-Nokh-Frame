package NokhXyr.NokhFrame;

import NokhXyr.NokhFrame.mixin.WalkAnimationStateAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Renders the real player temporarily posed for the studio, then restores every changed field. */
public final class StudioAvatarRenderer {
    public enum Motion { IDLE, WALK, RUN, SNEAK, ATTACK }

    private static boolean renderingMotion;
    private static boolean crouching;
    private static Motion currentMotion = Motion.IDLE;

    private StudioAvatarRenderer() {
    }

    public static void render(GuiGraphics graphics, LocalPlayer player, int left, int top, int right, int bottom,
                              int scale, float yaw, float pitch, float roll, Motion motion, float elapsedSeconds) {
        float playerYaw = 180.0F;
        float oldBody = player.yBodyRot;
        float oldBodyPrevious = player.yBodyRotO;
        float oldYaw = player.getYRot();
        float oldYawPrevious = player.yRotO;
        float oldHead = player.yHeadRot;
        float oldHeadPrevious = player.yHeadRotO;
        float oldPitch = player.getXRot();
        float oldPitchPrevious = player.xRotO;
        double oldXPrevious = player.xOld;
        double oldYPrevious = player.yOld;
        double oldZPrevious = player.zOld;
        float oldAttack = player.attackAnim;
        float oldAttackPrevious = player.oAttackAnim;
        WalkAnimationState walk = player.walkAnimation;
        WalkAnimationStateAccessor access = (WalkAnimationStateAccessor) walk;
        float oldWalkSpeed = walk.speed();
        float oldWalkSpeedPrevious = access.nokhframe$getSpeedOld();
        float oldWalkPosition = walk.position();

        graphics.enableScissor(left, top, right, bottom);
        try {
            renderingMotion = true;
            currentMotion = motion;
            crouching = motion == Motion.SNEAK;
            player.yBodyRot = playerYaw;
            player.yBodyRotO = playerYaw;
            player.setYRot(playerYaw);
            player.yRotO = playerYaw;
            player.yHeadRot = playerYaw;
            player.yHeadRotO = playerYaw;
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
            walk.setSpeed(speed);
            access.nokhframe$setSpeedOld(speed);
            access.nokhframe$setPosition(elapsedSeconds * 20.0F * speed);
            float attack = motion == Motion.ATTACK
                    ? Math.abs((elapsedSeconds * 1.4F % 2.0F) - 1.0F) : 0.0F;
            player.attackAnim = attack;
            player.oAttackAnim = attack;

            float entityScale = player.getScale();
            Vector3f translate = new Vector3f(0.0F, player.getBbHeight() / 2.0F + 0.0625F * entityScale, 0.0F);
            Quaternionf camera = new Quaternionf()
                    .rotateY((float) Math.toRadians(yaw))
                    .rotateX((float) Math.toRadians(pitch))
                    .rotateZ((float) Math.toRadians(roll));
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
            InventoryScreen.renderEntityInInventory(graphics, (left + right) / 2.0F, (top + bottom) / 2.0F,
                    scale / entityScale, translate, pose, camera, player);
            graphics.pose().pushPose();
            try {
                graphics.pose().translate((left + right) / 2.0F, (top + bottom) / 2.0F, 50.0F);
                graphics.pose().scale(scale / entityScale, scale / entityScale, -scale / entityScale);
                graphics.pose().translate(translate.x, translate.y, translate.z);
                graphics.pose().mulPose(pose);
                OwnedPetPreviewRenderer.render(graphics, player);
                WorldStagePreviewRenderer.renderBeforeParticles(graphics, player, camera);
                WorldParticleRenderer.render(graphics, player, camera);
                WorldStagePreviewRenderer.renderAfterParticles(graphics, player, camera);
            } finally {
                graphics.pose().popPose();
            }
        } finally {
            renderingMotion = false;
            currentMotion = Motion.IDLE;
            player.yBodyRot = oldBody;
            player.yBodyRotO = oldBodyPrevious;
            player.setYRot(oldYaw);
            player.yRotO = oldYawPrevious;
            player.yHeadRot = oldHead;
            player.yHeadRotO = oldHeadPrevious;
            player.setXRot(oldPitch);
            player.xRotO = oldPitchPrevious;
            player.xOld = oldXPrevious;
            player.yOld = oldYPrevious;
            player.zOld = oldZPrevious;
            player.attackAnim = oldAttack;
            player.oAttackAnim = oldAttackPrevious;
            walk.setSpeed(oldWalkSpeed);
            access.nokhframe$setSpeedOld(oldWalkSpeedPrevious);
            access.nokhframe$setPosition(oldWalkPosition);
            graphics.disableScissor();
        }
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
}
