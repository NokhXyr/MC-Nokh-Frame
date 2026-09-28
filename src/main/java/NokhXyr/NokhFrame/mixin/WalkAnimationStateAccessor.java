package NokhXyr.NokhFrame.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {
    @Accessor("speedOld")
    float nokhframe$getSpeedOld();

    @Accessor("speedOld")
    void nokhframe$setSpeedOld(float value);

    @Accessor("position")
    void nokhframe$setPosition(float value);
}
