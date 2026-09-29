package NokhXyr.NokhFrame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * A 3D set rendered around the studio player. Its origin is the player's feet; one unit is one block.
 * Scenes are loaded once and hold their own GPU resources until {@link #close()}.
 */
public interface StudioScene extends AutoCloseable {
    SceneMesh mesh();

    /** World view: streamed through the level buffer source (shaderpack compatible). */
    default void render(PoseStack pose, MultiBufferSource buffers) {
        mesh().renderImmediate(pose, buffers);
    }

    /** Studio view: drawn from cached GPU buffers. */
    default void renderCached(PoseStack pose) {
        mesh().renderCached(pose);
    }

    @Override
    void close();

    static StudioScene load(Path path) throws IOException, SceneException {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".nbt")) return StructureScene.load(path);
        if (name.endsWith(".bbmodel") || name.endsWith(".json")) return BlockbenchScene.load(path);
        throw new SceneException("status.nokhframe.invalid_scene_file");
    }

    /** Load failure carrying a translation key for the studio status line. */
    final class SceneException extends Exception {
        private final String translationKey;

        public SceneException(String translationKey) {
            super(translationKey);
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }
}
