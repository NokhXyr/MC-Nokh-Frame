package NokhXyr.NokhFrame;

import net.minecraft.commands.Commands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@Mod(value = NokhFrameMod.MOD_ID, dist = Dist.CLIENT)
public final class NokhFrameClient {
    private static NokhFrameClient instance;
    private boolean openRequested;

    public NokhFrameClient(IEventBus modBus) {
        instance = this;
        NeoForge.EVENT_BUS.addListener(this::registerClientCommands);
        StudioWorldView.register();
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::afterScreenRender);
        NeoForge.EVENT_BUS.addListener(this::showNativeNameplate);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::beginLevelPreview);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::endLevelPreview);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::beginClientPreview);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::endClientPreview);
    }

    /**
     * Servers running Nokh Frame (singleplayer included) provide {@code /nokhframe} themselves, filtered by
     * permission. Servers without the mod cannot check permissions, so this fallback is limited to operators.
     */
    private void registerClientCommands(RegisterClientCommandsEvent event) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && NetworkRegistry.hasChannel(connection, StudioAccessNetwork.OpenStudio.TYPE.id())) return;

        event.getDispatcher().register(Commands.literal("nokhframe")
                .requires(source -> source.hasPermission(StudioAccessNetwork.DEFAULT_PERMISSION_LEVEL))
                .executes(context -> {
                    requestOpen();
                    return 1;
                }));
    }

    public static void requestOpen() {
        if (instance != null) instance.openRequested = true;
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) openRequested = false;
        if (openRequested && minecraft.player != null && minecraft.screen == null) {
            openRequested = false;
            minecraft.setScreen(new StudioScreen());
        }
    }

    private void beginLevelPreview(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) StudioTickPreview.begin();
    }

    private void endLevelPreview(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) StudioTickPreview.clear();
    }

    private void beginClientPreview(ClientTickEvent.Post event) {
        StudioTickPreview.begin();
    }

    private void endClientPreview(ClientTickEvent.Post event) {
        StudioTickPreview.clear();
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
