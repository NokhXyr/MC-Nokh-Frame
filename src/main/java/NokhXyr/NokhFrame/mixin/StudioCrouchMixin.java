package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioAvatarRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
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
        }
    }
}
