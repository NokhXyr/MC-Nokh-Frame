package NokhXyr.NokhFrame;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
    private final SceneMesh mesh;

    private StructureScene(SceneMesh mesh) {
        this.mesh = mesh;
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
        float offsetX = -sizeX / 2.0F;
        float offsetY = -StudioRules.standingHeight(column);
        float offsetZ = -sizeZ / 2.0F;

        SceneMesh mesh = new SceneMesh();
        SceneMesh.Batch cutout = mesh.batch(Sheets.cutoutBlockSheet());
        SceneMesh.Batch translucent = mesh.batch(Sheets.translucentCullBlockSheet());
        RandomSource random = RandomSource.create();
        for (Map.Entry<BlockPos, BlockState> entry : states.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            if (state.getRenderShape() != RenderShape.MODEL) continue;
            BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
            float x = pos.getX() + offsetX;
            float y = pos.getY() + offsetY;
            float z = pos.getZ() + offsetZ;
            long seed = state.getSeed(pos);
            for (RenderType renderType : model.getRenderTypes(state, random.fork(), ModelData.EMPTY)) {
                SceneMesh.Batch batch = renderType == RenderType.translucent() ? translucent : cutout;
                for (Direction side : Direction.values()) {
                    if (!Block.shouldRenderFace(state, level, pos, side, pos.relative(side))) continue;
                    random.setSeed(seed);
                    for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, renderType)) {
                        addQuad(batch, quad, tint(state, quad), x, y, z);
                    }
                }
                random.setSeed(seed);
                for (BakedQuad quad : model.getQuads(state, null, random, ModelData.EMPTY, renderType)) {
                    addQuad(batch, quad, tint(state, quad), x, y, z);
                }
            }
        }
        if (mesh.isEmpty()) throw new SceneException("status.nokhframe.empty_scene");
        return new StructureScene(mesh);
    }

    private static int tint(BlockState state, BakedQuad quad) {
        if (!quad.isTinted()) return -1;
        // No biome is available outside a world, so tinted blocks use Minecraft's default grass and foliage colors.
        return 0xFF000000 | Minecraft.getInstance().getBlockColors().getColor(state, null, null, quad.getTintIndex());
    }

    /** Copies a baked quad (block vertex format: position, color, uv, light, packed normal) into the mesh. */
    private static void addQuad(SceneMesh.Batch batch, BakedQuad quad, int color, float x, float y, float z) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        Direction face = quad.getDirection();
        for (int i = 0; i < 4; i++) {
            int base = i * stride;
            int packedNormal = vertices[base + 7];
            float nx = (byte) (packedNormal & 255) / 127.0F;
            float ny = (byte) (packedNormal >> 8 & 255) / 127.0F;
            float nz = (byte) (packedNormal >> 16 & 255) / 127.0F;
            if (nx == 0.0F && ny == 0.0F && nz == 0.0F) {
                nx = face.getStepX();
                ny = face.getStepY();
                nz = face.getStepZ();
            }
            batch.vertex(x + Float.intBitsToFloat(vertices[base]), y + Float.intBitsToFloat(vertices[base + 1]),
                    z + Float.intBitsToFloat(vertices[base + 2]), color,
                    Float.intBitsToFloat(vertices[base + 4]), Float.intBitsToFloat(vertices[base + 5]), nx, ny, nz);
        }
    }

    @Override
    public SceneMesh mesh() {
        return mesh;
    }

    @Override
    public void close() {
        mesh.close();
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
