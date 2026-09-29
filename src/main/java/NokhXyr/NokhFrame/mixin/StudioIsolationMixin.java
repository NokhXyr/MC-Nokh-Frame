package NokhXyr.NokhFrame.mixin;

import NokhXyr.NokhFrame.StudioWorldView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Studio view rendered through the world pipeline (shaderpack active): hides terrain, sky, clouds and weather so
 * only the player, the 3D scene and the studio backdrop remain. Applied after Sodium (priority 1100) so it also
 * wraps Sodium's replacement of the terrain pass.
 */
@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class StudioIsolationMixin {
    @Inject(method = "renderSectionLayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void nokhframe$hideTerrain(RenderType renderType, double x, double y, double z, Matrix4f frustum,
                                       Matrix4f projection, CallbackInfo info) {
        if (StudioWorldView.isolating()) info.cancel();
    }

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true, require = 0)
    private void nokhframe$hideSky(Matrix4f frustum, Matrix4f projection, float partialTick, Camera camera,
                                   boolean foggy, Runnable fogSetup, CallbackInfo info) {
        if (StudioWorldView.isolating()) info.cancel();
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 0)
    private void nokhframe$hideClouds(PoseStack pose, Matrix4f frustum, Matrix4f projection, float partialTick,
                                      double x, double y, double z, CallbackInfo info) {
        if (StudioWorldView.isolating()) info.cancel();
    }

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true, require = 0)
    private void nokhframe$hideWeather(LightTexture lightTexture, float partialTick, double x, double y, double z,
                                       CallbackInfo info) {
        if (StudioWorldView.isolating()) info.cancel();
    }
}
