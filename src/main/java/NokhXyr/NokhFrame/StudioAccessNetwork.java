package NokhXyr.NokhFrame;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Server-side access control. {@code /nokhframe} is a server command guarded by the {@code nokhframe.use}
 * permission, so players without it never receive the command in their tree and get no auto-completion.
 * Without a permission mod the node defaults to operators (level 2) and the singleplayer / LAN host.
 */
public final class StudioAccessNetwork {
    public static final int DEFAULT_PERMISSION_LEVEL = 2;

    public static final PermissionNode<Boolean> USE_STUDIO = new PermissionNode<>(
            NokhFrameMod.MOD_ID, "use", PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> player != null && isOperatorOrHost(player))
            .setInformation(Component.literal("Use Nokh Frame"), Component.literal("Open the Nokh Frame photo studio"));

    private StudioAccessNetwork() {
    }

    public static boolean isOperatorOrHost(ServerPlayer player) {
        return player.server.isSingleplayerOwner(player.getGameProfile()) || player.hasPermissions(DEFAULT_PERMISSION_LEVEL);
    }

    public static boolean canUse(ServerPlayer player) {
        return PermissionAPI.getPermission(player, USE_STUDIO);
    }

    public static void registerPermissions(PermissionGatherEvent.Nodes event) {
        event.addNodes(USE_STUDIO);
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nokhframe")
                .requires(StudioAccessNetwork::canUseCommand)
                .executes(context -> openStudio(context.getSource())));
    }

    private static boolean canUseCommand(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player != null && canUse(player);
    }

    private static int openStudio(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        if (!NetworkRegistry.hasChannel(player.connection, OpenStudio.TYPE.id())) {
            source.sendFailure(Component.translatableWithFallback("message.nokhframe.client_required",
                    "Install Nokh Frame on your client to use the studio"));
            return 0;
        }
        PacketDistributor.sendToPlayer(player, OpenStudio.INSTANCE);
        return Command.SINGLE_SUCCESS;
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional()
                .playToClient(OpenStudio.TYPE, OpenStudio.STREAM_CODEC, StudioAccessNetwork::handleOpenStudio);
    }

    private static void handleOpenStudio(OpenStudio payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NokhFrameClient.requestOpen();
        }
    }

    public record OpenStudio() implements CustomPacketPayload {
        public static final OpenStudio INSTANCE = new OpenStudio();
        public static final Type<OpenStudio> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(NokhFrameMod.MOD_ID, "open_studio"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenStudio> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
