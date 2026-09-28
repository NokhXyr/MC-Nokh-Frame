package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exposes local player effects that mods hide in first-person view while the studio is open. */
@Mixin(Options.class)
public abstract class StudioPerspectiveMixin {
    @Inject(method = "getCameraType", at = @At("HEAD"), cancellable = true)
    private void nokhframe$studioPerspective(CallbackInfoReturnable<CameraType> result) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.screen instanceof StudioScreen) {
            result.setReturnValue(CameraType.THIRD_PERSON_BACK);
        }
    }
}
