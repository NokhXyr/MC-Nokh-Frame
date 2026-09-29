package NokhXyr.NokhFrame;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Vanilla structure file ({@code .nbt}, as saved by a structure block) rendered as the studio set. */
final class StructureScene implements StudioScene {
    private final List<PlacedBlock> blocks;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;

    private StructureScene(List<PlacedBlock> blocks, float offsetX, float offsetY, float offsetZ) {
        this.blocks = blocks;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    static StructureScene load(Path path) throws IOException, SceneException {
        CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.create(StudioRules.MAX_SCENE_BYTES * 8L));
        Minecraft minecraft = Minecraft.getInstance();
        int version = NbtUtils.getDataVersion(tag, 500);
        if (version < SharedConstants.getCurrentVersion().getDataVersion().getVersion()) {
            tag = DataFixTypes.STRUCTURE.updateToCurrentVersion(minecraft.getFixerUpper(), tag, version);
        }

        ListTag size = tag.getList("size", Tag.TAG_INT);
        if (size.size() != 3) throw new SceneException("status.nokhframe.invalid_scene_file");
        int sizeX = size.getInt(0);
        int sizeY = size.getInt(1);
        int sizeZ = size.getInt(2);
        if (!StudioRules.isSupportedStructureSize(sizeX, sizeY, sizeZ)) {
            throw new SceneException("status.nokhframe.invalid_scene_size");
        }

        ListTag paletteTag = tag.contains("palettes", Tag.TAG_LIST)
                ? tag.getList("palettes", Tag.TAG_LIST).getList(0)
                : tag.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), paletteTag.getCompound(i)));
        }

        ListTag blockTags = tag.getList("blocks", Tag.TAG_COMPOUND);
        if (blockTags.size() > StudioRules.MAX_STRUCTURE_BLOCKS) throw new SceneException("status.nokhframe.invalid_scene_size");
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (int i = 0; i < blockTags.size(); i++) {
            CompoundTag block = blockTags.getCompound(i);
            ListTag pos = block.getList("pos", Tag.TAG_INT);
            int index = block.getInt("state");
            if (pos.size() != 3 || index < 0 || index >= palette.size()) continue;
            BlockState state = palette.get(index);
            if (state.isAir() || state.is(Blocks.STRUCTURE_VOID)) continue;
            states.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), state);
        }
        if (states.isEmpty()) throw new SceneException("status.nokhframe.empty_scene");

        SceneBlocks level = new SceneBlocks(states);
        int centerX = sizeX / 2;
        int centerZ = sizeZ / 2;
        boolean[] column = new boolean[sizeY];
        for (int y = 0; y < sizeY; y++) {
            BlockState state = level.getBlockState(new BlockPos(centerX, y, centerZ));
            column[y] = !state.getCollisionShape(level, new BlockPos(centerX, y, centerZ)).isEmpty();
        }
        int feet = StudioRules.standingHeight(column);

        List<PlacedBlock> placed = new ArrayList<>();
        RandomSource random = RandomSource.create();
        for (Map.Entry<BlockPos, BlockState> entry : states.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            if (state.getRenderShape() != RenderShape.MODEL) continue;
            BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
            List<SceneQuad> quads = new ArrayList<>();
            long seed = state.getSeed(pos);
            for (RenderType renderType : model.getRenderTypes(state, random.fork(), ModelData.EMPTY)) {
                boolean translucent = renderType == RenderType.translucent();
                for (Direction side : Direction.values()) {
                    if (!Block.shouldRenderFace(state, level, pos, side, pos.relative(side))) continue;
                    random.setSeed(seed);
                    for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, renderType)) {
                        quads.add(new SceneQuad(quad, tint(state, level, pos, quad), translucent));
                    }
                }
                random.setSeed(seed);
                for (BakedQuad quad : model.getQuads(state, null, random, ModelData.EMPTY, renderType)) {
                    quads.add(new SceneQuad(quad, tint(state, level, pos, quad), translucent));
                }
            }
            if (!quads.isEmpty()) placed.add(new PlacedBlock(pos, quads));
        }
        return new StructureScene(placed, -sizeX / 2.0F, -feet, -sizeZ / 2.0F);
    }

    private static int tint(BlockState state, BlockGetter level, BlockPos pos, BakedQuad quad) {
        if (!quad.isTinted()) return -1;
        // No biome is available outside a world, so tinted blocks use Minecraft's default grass and foliage colors.
        return Minecraft.getInstance().getBlockColors().getColor(state, null, null, quad.getTintIndex());
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer cutout = buffers.getBuffer(Sheets.cutoutBlockSheet());
        emit(pose, cutout, false);
        VertexConsumer translucent = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        emit(pose, translucent, true);
    }

    private void emit(PoseStack pose, VertexConsumer consumer, boolean translucent) {
        for (PlacedBlock block : blocks) {
            pose.pushPose();
            pose.translate(block.pos.getX() + offsetX, block.pos.getY() + offsetY, block.pos.getZ() + offsetZ);
            for (SceneQuad quad : block.quads) {
                if (quad.translucent != translucent) continue;
                float red = (quad.color >> 16 & 255) / 255.0F;
                float green = (quad.color >> 8 & 255) / 255.0F;
                float blue = (quad.color & 255) / 255.0F;
                consumer.putBulkData(pose.last(), quad.quad, red, green, blue, 1.0F,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }
    }

    @Override
    public void close() {
    }

    private record PlacedBlock(BlockPos pos, List<SceneQuad> quads) {
    }

    private record SceneQuad(BakedQuad quad, int color, boolean translucent) {
    }

    /** Just enough of a world for face culling and collision shapes. */
    private record SceneBlocks(Map<BlockPos, BlockState> states) implements BlockGetter {
        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 4096;
        }

        @Override
        public int getMinBuildHeight() {
            return -2048;
        }
    }
}
