package NokhXyr.NokhFrame;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/** Common entry point: server command, permission node and the optional open-studio packet. */
@Mod(NokhFrameMod.MOD_ID)
public final class NokhFrameMod {
    public static final String MOD_ID = "nokhframe";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NokhFrameMod(IEventBus modBus) {
        modBus.addListener(StudioAccessNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(StudioAccessNetwork::registerPermissions);
        NeoForge.EVENT_BUS.addListener(StudioAccessNetwork::registerCommands);
    }
}
