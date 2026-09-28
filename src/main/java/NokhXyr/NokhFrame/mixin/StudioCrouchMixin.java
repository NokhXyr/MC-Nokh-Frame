package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioAvatarRenderer;
import NokhXyr.NokhFrame.StudioTickPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class StudioCrouchMixin {
    @Inject(method = "isCrouching", at = @At("HEAD"), cancellable = true)
    private void nokhframe$simulatedCrouch(CallbackInfoReturnable<Boolean> result) {
        if (StudioAvatarRenderer.isRenderingMotion() && (Object) this == Minecraft.getInstance().player) {
            result.setReturnValue(StudioAvatarRenderer.isCrouching());
        } else if (StudioTickPreview.simulatesMovementFor(this)) {
            result.setReturnValue(StudioTickPreview.isCrouching());
        }
    }

    @Inject(method = "isSprinting", at = @At("HEAD"), cancellable = true)
    private void nokhframe$simulatedSprint(CallbackInfoReturnable<Boolean> result) {
        if (StudioAvatarRenderer.isRenderingMotion() && (Object) this == Minecraft.getInstance().player) {
            result.setReturnValue(StudioAvatarRenderer.isRunning());
        } else if (StudioTickPreview.simulatesMovementFor(this)) {
            result.setReturnValue(StudioTickPreview.isRunning());
        }
    }

    @Inject(method = "getDeltaMovement", at = @At("HEAD"), cancellable = true)
    private void nokhframe$simulatedVelocity(CallbackInfoReturnable<Vec3> result) {
        if (StudioAvatarRenderer.isRenderingMotion() && (Object) this == Minecraft.getInstance().player) {
            result.setReturnValue(StudioAvatarRenderer.simulatedDelta());
        } else if (StudioTickPreview.simulatesMovementFor(this)) {
            result.setReturnValue(StudioTickPreview.simulatedDelta());
        }
    }

    @Inject(method = "position", at = @At("RETURN"), cancellable = true)
    private void nokhframe$simulatedPosition(CallbackInfoReturnable<Vec3> result) {
        if (StudioTickPreview.simulatesMovementFor(this)) {
            result.setReturnValue(StudioTickPreview.offsetPosition(result.getReturnValue()));
        }
    }
}
