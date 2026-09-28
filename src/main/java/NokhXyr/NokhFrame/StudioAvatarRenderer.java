package NokhXyr.NokhFrame;

import NokhXyr.NokhFrame.mixin.WalkAnimationStateAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.WalkAnimationState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Renders the real player temporarily posed for the studio, then restores every changed field. */
public final class StudioAvatarRenderer {
    public enum Motion { IDLE, WALK, RUN, SNEAK, ATTACK }

    private static boolean renderingMotion;
    private static boolean crouching;

    private StudioAvatarRenderer() {
    }

    public static void render(GuiGraphics graphics, LocalPlayer player, int left, int top, int right, int bottom,
                              int scale, float angle, Motion motion, float elapsedSeconds) {
        float yaw = 180.0F + angle * 20.0F;
        float oldBody = player.yBodyRot;
        float oldBodyPrevious = player.yBodyRotO;
        float oldYaw = player.getYRot();
        float oldHead = player.yHeadRot;
        float oldHeadPrevious = player.yHeadRotO;
        float oldPitch = player.getXRot();
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
            crouching = motion == Motion.SNEAK;
            player.yBodyRot = yaw;
            player.yBodyRotO = yaw;
            player.setYRot(yaw);
            player.yHeadRot = yaw;
            player.yHeadRotO = yaw;
            player.setXRot(0.0F);
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
            Quaternionf camera = new Quaternionf();
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
            InventoryScreen.renderEntityInInventory(graphics, (left + right) / 2.0F, (top + bottom) / 2.0F,
                    scale / entityScale, translate, pose, camera, player);
        } finally {
            renderingMotion = false;
            player.yBodyRot = oldBody;
            player.yBodyRotO = oldBodyPrevious;
            player.setYRot(oldYaw);
            player.yHeadRot = oldHead;
            player.yHeadRotO = oldHeadPrevious;
            player.setXRot(oldPitch);
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
}
