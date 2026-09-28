package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioTickPreview;
import com.google.common.collect.Iterables;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Exposes a preview-only item to client effect emitters that inspect dropped items. */
@Mixin(ClientLevel.class)
public abstract class StudioItemEntityMixin {
    @Inject(method = "entitiesForRendering", at = @At("RETURN"), cancellable = true)
    private void nokhframe$previewItemEntity(CallbackInfoReturnable<Iterable<Entity>> result) {
        ItemEntity preview = StudioTickPreview.previewItemEntity();
        if (preview != null) {
            result.setReturnValue(Iterables.concat(result.getReturnValue(), List.of(preview)));
        }
    }
}
