package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioAvatarRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drives the visible limbs even when another renderer ignores walkAnimation. */
@Mixin(HumanoidModel.class)
public abstract class StudioHumanoidMotionMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void nokhframe$studioMotion(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                        float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo info) {
        if (!StudioAvatarRenderer.isRenderingMotion() || entity != Minecraft.getInstance().player) return;
        StudioAvatarRenderer.Motion motion = StudioAvatarRenderer.currentMotion();
        if (motion != StudioAvatarRenderer.Motion.WALK && motion != StudioAvatarRenderer.Motion.RUN
                && motion != StudioAvatarRenderer.Motion.SNEAK) return;

        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        float seconds = StudioAvatarRenderer.motionSeconds();
        float frequency = motion == StudioAvatarRenderer.Motion.RUN ? 17.0F : 11.0F;
        float amplitude = switch (motion) {
            case RUN -> 1.0F;
            case SNEAK -> 0.35F;
            default -> 0.7F;
        };
        float swing = (float) Math.cos(seconds * frequency) * amplitude;
        model.rightArm.xRot = -swing;
        model.leftArm.xRot = swing;
        model.rightLeg.xRot = swing;
        model.leftLeg.xRot = -swing;
        if (motion == StudioAvatarRenderer.Motion.SNEAK) {
            model.rightArm.xRot += 0.4F;
            model.leftArm.xRot += 0.4F;
        }
    }
}
