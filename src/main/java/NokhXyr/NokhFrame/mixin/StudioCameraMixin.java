package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioWorldView;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** World view: orbits the real game camera around the player so shaderpacks render the studio shot. */
@Mixin(Camera.class)
public abstract class StudioCameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot, float roll);

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void move(float zoom, float dy, float dx);

    @Inject(method = "setup", at = @At("TAIL"))
    private void nokhframe$studioOrbit(BlockGetter level, Entity entity, boolean detached, boolean mirrored,
                                       float partialTick, CallbackInfo info) {
        StudioWorldView.Orbit orbit = StudioWorldView.orbit(entity, partialTick);
        if (orbit == null) return;
        setRotation(orbit.yaw(), orbit.pitch(), orbit.roll());
        setPosition(orbit.target());
        move(-orbit.distance(), 0.0F, orbit.rightShift());
    }
}
