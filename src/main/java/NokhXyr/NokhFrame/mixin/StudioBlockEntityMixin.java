package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioWorldView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the world's chests, signs and other block entities while the studio view is isolated from the world. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class StudioBlockEntityMixin {
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nokhframe$hideBlockEntity(BlockEntity blockEntity, float partialTick, PoseStack pose,
                                           MultiBufferSource buffers, CallbackInfo info) {
        if (StudioWorldView.isolating()) info.cancel();
    }
}
