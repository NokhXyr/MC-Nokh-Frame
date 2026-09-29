package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioAvatarRenderer;
import NokhXyr.NokhFrame.StudioPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the studio pose after model setup, immediately before drawing the player. */
@Mixin(AgeableListModel.class)
public abstract class StudioPlayerModelRenderMixin {
    @Inject(method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"))
    private void nokhframe$renderMotion(PoseStack pose, VertexConsumer vertices, int light, int overlay,
                                        int color, CallbackInfo info) {
        if (!StudioAvatarRenderer.isPosingModel() || !((Object) this instanceof PlayerModel<?> model)) return;
        StudioPose studioPose = StudioAvatarRenderer.activePose();
        if (studioPose != null && studioPose.active()) {
            // Armor and cosmetic layers copy these rotations from the player model when they render.
            nokhframe$rotate(model.head, studioPose, StudioPose.Part.HEAD);
            nokhframe$rotate(model.body, studioPose, StudioPose.Part.BODY);
            nokhframe$rotate(model.rightArm, studioPose, StudioPose.Part.RIGHT_ARM);
            nokhframe$rotate(model.leftArm, studioPose, StudioPose.Part.LEFT_ARM);
            nokhframe$rotate(model.rightLeg, studioPose, StudioPose.Part.RIGHT_LEG);
            nokhframe$rotate(model.leftLeg, studioPose, StudioPose.Part.LEFT_LEG);
            model.hat.copyFrom(model.head);
            model.jacket.copyFrom(model.body);
            model.rightSleeve.copyFrom(model.rightArm);
            model.leftSleeve.copyFrom(model.leftArm);
            model.rightPants.copyFrom(model.rightLeg);
            model.leftPants.copyFrom(model.leftLeg);
            return;
        }
        StudioAvatarRenderer.Motion motion = StudioAvatarRenderer.currentMotion();
        if (motion != StudioAvatarRenderer.Motion.WALK && motion != StudioAvatarRenderer.Motion.RUN
                && motion != StudioAvatarRenderer.Motion.SNEAK) return;

        float seconds = StudioAvatarRenderer.motionSeconds();
        float frequency = motion == StudioAvatarRenderer.Motion.RUN ? 17.0F : 11.0F;
        float amplitude = switch (motion) {
            case RUN -> 1.0F;
            case SNEAK -> 0.35F;
            default -> 0.7F;
        };
        float swing = (float) Math.cos(seconds * frequency) * amplitude;
        model.rightArm.xRot = -swing + (motion == StudioAvatarRenderer.Motion.SNEAK ? 0.4F : 0.0F);
        model.leftArm.xRot = swing + (motion == StudioAvatarRenderer.Motion.SNEAK ? 0.4F : 0.0F);
        model.rightLeg.xRot = swing;
        model.leftLeg.xRot = -swing;
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    private static void nokhframe$rotate(ModelPart part, StudioPose pose, StudioPose.Part which) {
        part.xRot = StudioPose.radians(pose.angle(which, 0));
        part.yRot = StudioPose.radians(pose.angle(which, 1));
        part.zRot = StudioPose.radians(pose.angle(which, 2));
    }
}
