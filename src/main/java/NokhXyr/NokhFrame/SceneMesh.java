package NokhXyr.NokhFrame;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scene geometry in scene space (one unit per block, origin at the player's feet), grouped by render type.
 * The studio view draws it from GPU buffers built once, so large scenes stay smooth; the world view streams it
 * through the level's buffer source instead, which shaderpacks (Iris/Oculus) can intercept.
 */
final class SceneMesh implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 9;
    // Minecraft's inventory entity lights (Lighting.INVENTORY_DIFFUSE_LIGHT_0/1), in GUI view space.
    private static final Vector3f GUI_LIGHT_0 = new Vector3f(0.2F, -1.0F, 1.0F).normalize();
    private static final Vector3f GUI_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.0F).normalize();

    private final List<Batch> batches = new ArrayList<>();

    Batch batch(RenderType type) {
        for (Batch batch : batches) {
            if (batch.type == type) return batch;
        }
        Batch batch = new Batch(type);
        batches.add(batch);
        return batch;
    }

    boolean isEmpty() {
        for (Batch batch : batches) {
            if (batch.floats > 0) return false;
        }
        return true;
    }

    /** World view: vertices go through the frame's buffer source like any other entity geometry. */
    void renderImmediate(PoseStack pose, MultiBufferSource buffers) {
        PoseStack.Pose last = pose.last();
        Matrix4f matrix = last.pose();
        for (Batch batch : batches) {
            if (batch.floats == 0) continue;
            VertexConsumer consumer = buffers.getBuffer(batch.type);
            float[] d = batch.data;
            for (int i = 0; i < batch.floats; i += FLOATS_PER_VERTEX) {
                consumer.addVertex(matrix, d[i], d[i + 1], d[i + 2])
                        .setColor(Float.floatToRawIntBits(d[i + 3]))
                        .setUv(d[i + 4], d[i + 5])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(last, d[i + 6], d[i + 7], d[i + 8]);
            }
        }
    }

    /**
     * Studio view: draws cached GPU buffers. Normals stay in scene space on the GPU, so the GUI light directions
     * are brought into scene space with the inverse normal matrix to keep the usual inventory shading.
     */
    void renderCached(PoseStack pose) {
        Matrix3f toScene = new Matrix3f(pose.last().normal()).invert();
        RenderSystem.setShaderLights(toScene.transform(new Vector3f(GUI_LIGHT_0)).normalize(),
                toScene.transform(new Vector3f(GUI_LIGHT_1)).normalize());
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        for (Batch batch : batches) {
            VertexBuffer buffer = batch.upload();
            if (buffer == null) continue;
            batch.type.setupRenderState();
            try {
                ShaderInstance shader = RenderSystem.getShader();
                if (shader != null) {
                    buffer.bind();
                    buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader);
                    VertexBuffer.unbind();
                }
            } finally {
                batch.type.clearRenderState();
            }
        }
    }

    @Override
    public void close() {
        for (Batch batch : batches) batch.release();
    }

    static final class Batch {
        private final RenderType type;
        private float[] data = new float[FLOATS_PER_VERTEX * 256];
        private int floats;
        private @Nullable VertexBuffer gpu;
        private boolean uploadFailed;

        private Batch(RenderType type) {
            this.type = type;
        }

        void vertex(float x, float y, float z, int argb, float u, float v, float nx, float ny, float nz) {
            if (floats + FLOATS_PER_VERTEX > data.length) data = Arrays.copyOf(data, data.length * 2);
            data[floats++] = x;
            data[floats++] = y;
            data[floats++] = z;
            data[floats++] = Float.intBitsToFloat(argb);
            data[floats++] = u;
            data[floats++] = v;
            data[floats++] = nx;
            data[floats++] = ny;
            data[floats++] = nz;
        }

        private @Nullable VertexBuffer upload() {
            if (gpu != null || uploadFailed || floats == 0) return gpu;
            int vertices = floats / FLOATS_PER_VERTEX;
            try (ByteBufferBuilder bytes = new ByteBufferBuilder(vertices * DefaultVertexFormat.NEW_ENTITY.getVertexSize())) {
                BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
                for (int i = 0; i < floats; i += FLOATS_PER_VERTEX) {
                    builder.addVertex(data[i], data[i + 1], data[i + 2])
                            .setColor(Float.floatToRawIntBits(data[i + 3]))
                            .setUv(data[i + 4], data[i + 5])
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightTexture.FULL_BRIGHT)
                            .setNormal(data[i + 6], data[i + 7], data[i + 8]);
                }
                MeshData mesh = builder.build();
                if (mesh == null) {
                    uploadFailed = true;
                    return null;
                }
                VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                buffer.bind();
                buffer.upload(mesh);
                VertexBuffer.unbind();
                gpu = buffer;
            } catch (RuntimeException exception) {
                NokhFrameMod.LOGGER.warn("Could not upload studio scene geometry", exception);
                uploadFailed = true;
            }
            return gpu;
        }

        private void release() {
            if (gpu != null) {
                gpu.close();
                gpu = null;
            }
        }
    }
}
