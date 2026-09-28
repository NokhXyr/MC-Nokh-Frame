package NokhXyr.NokhFrame;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import org.lwjgl.glfw.GLFW;

@Mod(value = NokhFrameMod.MOD_ID, dist = Dist.CLIENT)
public final class NokhFrameClient {
    private static final KeyMapping OPEN_STUDIO = new KeyMapping(
            "key.nokhframe.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "key.categories.nokhframe"
    );

    public NokhFrameClient(IEventBus modBus) {
        modBus.addListener(this::registerKeys);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::afterScreenRender);
        NeoForge.EVENT_BUS.addListener(this::showNativeNameplate);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STUDIO);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_STUDIO.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new StudioScreen());
            }
        }
    }

    private void afterScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof StudioScreen studio) || !studio.takeCapture()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        event.getGuiGraphics().flush();
        Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(), message ->
                minecraft.execute(() -> {
                    if (minecraft.screen == studio) {
                        studio.showCaptureResult(message);
                    }
                    if (minecraft.player != null) {
                        minecraft.player.displayClientMessage(message, false);
                    }
                })
        );
    }

    private void showNativeNameplate(RenderNameTagEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (SkinOverride.isRenderingAvatar() && event.getEntity() == minecraft.player) {
            event.setCanRender(TriState.TRUE);
        }
    }
}
