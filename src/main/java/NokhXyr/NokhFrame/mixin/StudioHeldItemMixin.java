package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioTickPreview;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets existing client particle emitters inspect the selected preview stack. */
@Mixin(LivingEntity.class)
public abstract class StudioHeldItemMixin {
    @Inject(method = "getMainHandItem", at = @At("HEAD"), cancellable = true)
    private void nokhframe$previewHeldItem(CallbackInfoReturnable<ItemStack> result) {
        if (StudioTickPreview.appliesTo(this) && !StudioTickPreview.previewHeldItem().isEmpty()) {
            result.setReturnValue(StudioTickPreview.previewHeldItem());
        }
    }
}
