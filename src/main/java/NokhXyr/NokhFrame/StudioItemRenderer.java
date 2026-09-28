package NokhXyr.NokhFrame;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class StudioItemRenderer {
    private StudioItemRenderer() {
    }

    public static void render(GuiGraphics graphics, ItemStack stack, int stageRight, int height,
                              float yaw, float pitch, float roll, float zoom) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0);
        float size = Math.min(stageRight, height) * 0.50F * zoom;
        graphics.enableScissor(0, 0, stageRight, height);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(stageRight / 2.0F, height / 2.0F, 150.0F);
            graphics.pose().scale(size, -size, size);
            Quaternionf rotation = new Quaternionf()
                    .rotateY((float) Math.toRadians(yaw))
                    .rotateX((float) Math.toRadians(pitch))
                    .rotateZ((float) Math.toRadians(roll));
            graphics.pose().mulPose(rotation);
            if (!model.usesBlockLight()) Lighting.setupForFlatItems();
            minecraft.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, graphics.pose(),
                    graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, model);
            graphics.flush();
            Lighting.setupFor3DItems();
            if (minecraft.player != null) {
                Vec3 emissionCenter = minecraft.player.position()
                        .add(0.0, minecraft.player.getBbHeight() * 0.5, 0.0);
                WorldParticleRenderer.render(graphics, emissionCenter, rotation, 0.9);
            }
        } finally {
            Lighting.setupFor3DItems();
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }
}
