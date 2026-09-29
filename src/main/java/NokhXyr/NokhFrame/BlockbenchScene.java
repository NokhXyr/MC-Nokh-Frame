package NokhXyr.NokhFrame;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Blockbench model rendered as the studio set: a {@code .bbmodel} project (textures embedded) or a
 * Java block/item {@code .json} export. Cubes are supported with element, group and face rotations.
 * 16 model pixels are one block; the model origin is the player's feet.
 */
final class BlockbenchScene implements StudioScene {
    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};
    private static int nextTextureId;

    private final List<TextureBatch> batches;
    private final SceneMesh mesh;

    private BlockbenchScene(List<TextureBatch> batches, SceneMesh mesh) {
        this.batches = batches;
        this.mesh = mesh;
    }

    static BlockbenchScene load(Path path) throws IOException, SceneException {
        JsonObject root;
        try {
            root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException exception) {
            throw new SceneException("status.nokhframe.invalid_scene_file");
        }
        if (!root.has("elements") || !root.get("elements").isJsonArray()) {
            throw new SceneException("status.nokhframe.scene_no_elements");
        }
        boolean project = root.has("meta");
        List<TextureBatch> batches = new ArrayList<>();
        try {
            Builder builder = project ? fromProject(root, batches) : fromJavaModel(root, path, batches);
            builder.build();
        } catch (SceneException | RuntimeException exception) {
            batches.forEach(TextureBatch::release);
            throw exception;
        }
        batches.removeIf(batch -> {
            if (batch.vertices.isEmpty()) batch.release();
            return batch.vertices.isEmpty();
        });
        if (batches.isEmpty()) throw new SceneException("status.nokhframe.scene_no_elements");
        SceneMesh mesh = new SceneMesh();
        for (TextureBatch batch : batches) {
            SceneMesh.Batch geometry = mesh.batch(batch.renderType);
            for (float[] v : batch.vertices) geometry.vertex(v[0], v[1], v[2], -1, v[3], v[4], v[5], v[6], v[7]);
            batch.vertices.clear();
        }
        return new BlockbenchScene(batches, mesh);
    }

    // ---- .bbmodel project -------------------------------------------------------------------------------------

    private static Builder fromProject(JsonObject root, List<TextureBatch> batches) throws SceneException {
        JsonObject meta = root.getAsJsonObject("meta");
        String format = meta.has("model_format") ? meta.get("model_format").getAsString() : "free";
        boolean blockSpace = format.equals("java_block");
        int resolutionWidth = 16;
        int resolutionHeight = 16;
        if (root.has("resolution")) {
            JsonObject resolution = root.getAsJsonObject("resolution");
            resolutionWidth = resolution.has("width") ? resolution.get("width").getAsInt() : 16;
            resolutionHeight = resolution.has("height") ? resolution.get("height").getAsInt() : 16;
        }

        Map<String, Integer> textureById = new HashMap<>();
        JsonArray textures = root.has("textures") ? root.getAsJsonArray("textures") : new JsonArray();
        if (textures.size() > StudioRules.MAX_SCENE_TEXTURES) throw new SceneException("status.nokhframe.invalid_scene_size");
        for (int i = 0; i < textures.size(); i++) {
            JsonObject texture = textures.get(i).getAsJsonObject();
            NativeImage image = decodeDataUri(texture.has("source") ? texture.get("source").getAsString() : "");
            int uvWidth = texture.has("uv_width") ? texture.get("uv_width").getAsInt() : resolutionWidth;
            int uvHeight = texture.has("uv_height") ? texture.get("uv_height").getAsInt() : resolutionHeight;
            batches.add(TextureBatch.upload(image, uvWidth, uvHeight));
            if (texture.has("id")) textureById.put(texture.get("id").getAsString(), i);
        }

        Map<String, List<Transform>> groupChains = new HashMap<>();
        if (root.has("outliner")) collectGroups(root.getAsJsonArray("outliner"), new ArrayList<>(), groupChains, true);

        return () -> {
            JsonArray elements = root.getAsJsonArray("elements");
            if (elements.size() > StudioRules.MAX_SCENE_ELEMENTS) throw new SceneException("status.nokhframe.invalid_scene_size");
            for (JsonElement entry : elements) {
                JsonObject element = entry.getAsJsonObject();
                String type = element.has("type") ? element.get("type").getAsString() : "cube";
                if (!type.equals("cube")) continue;
                if (element.has("visibility") && !element.get("visibility").getAsBoolean()) continue;
                String uuid = element.has("uuid") ? element.get("uuid").getAsString() : "";
                List<Transform> chain = groupChains.getOrDefault(uuid, List.of());
                if (chain == null) continue; // inside a hidden group
                Vector3f from = vector(element, "from", 0.0F);
                Vector3f to = vector(element, "to", 0.0F);
                float inflate = element.has("inflate") ? element.get("inflate").getAsFloat() : 0.0F;
                from.sub(inflate, inflate, inflate);
                to.add(inflate, inflate, inflate);
                List<Transform> transforms = new ArrayList<>();
                transforms.add(new Transform(vector(element, "origin", 0.0F), eulerZyx(vector(element, "rotation", 0.0F))));
                transforms.addAll(chain);
                JsonObject faces = element.has("faces") ? element.getAsJsonObject("faces") : new JsonObject();
                for (int face = 0; face < FACES.length; face++) {
                    if (!faces.has(FACES[face])) continue;
                    JsonObject data = faces.getAsJsonObject(FACES[face]);
                    if (!data.has("texture") || data.get("texture").isJsonNull() || !data.has("uv")) continue;
                    JsonElement textureRef = data.get("texture");
                    Integer index = textureRef.getAsJsonPrimitive().isNumber()
                            ? Integer.valueOf(textureRef.getAsInt()) : textureById.get(textureRef.getAsString());
                    if (index == null || index < 0 || index >= batches.size()) continue;
                    TextureBatch batch = batches.get(index);
                    float[] uv = floats(data.getAsJsonArray("uv"), 4);
                    int rotation = data.has("rotation") ? data.get("rotation").getAsInt() : 0;
                    addFace(batch, face, from, to, uv, batch.uvWidth, batch.uvHeight, rotation, transforms, blockSpace);
                }
            }
        };
    }

    /** Maps element UUIDs to their enclosing group transforms (innermost first); hidden groups map to null. */
    private static void collectGroups(JsonArray children, List<Transform> parents, Map<String, List<Transform>> chains,
                                      boolean visible) {
        for (JsonElement child : children) {
            if (child.isJsonPrimitive()) {
                chains.put(child.getAsString(), visible ? List.copyOf(parents) : null);
            } else if (child.isJsonObject()) {
                JsonObject group = child.getAsJsonObject();
                boolean groupVisible = visible && (!group.has("visibility") || group.get("visibility").getAsBoolean());
                List<Transform> chain = new ArrayList<>();
                chain.add(new Transform(vector(group, "origin", 0.0F), eulerZyx(vector(group, "rotation", 0.0F))));
                chain.addAll(parents);
                if (group.has("children")) collectGroups(group.getAsJsonArray("children"), chain, chains, groupVisible);
            }
        }
    }

    // ---- Java block / item model ------------------------------------------------------------------------------

    private static Builder fromJavaModel(JsonObject root, Path path, List<TextureBatch> batches) throws SceneException {
        JsonObject textureMap = root.has("textures") ? root.getAsJsonObject("textures") : new JsonObject();
        Map<String, TextureBatch> byKey = new HashMap<>();
        return () -> {
            JsonArray elements = root.getAsJsonArray("elements");
            if (elements.size() > StudioRules.MAX_SCENE_ELEMENTS) throw new SceneException("status.nokhframe.invalid_scene_size");
            for (JsonElement entry : elements) {
                JsonObject element = entry.getAsJsonObject();
                Vector3f from = vector(element, "from", 0.0F);
                Vector3f to = vector(element, "to", 16.0F);
                List<Transform> transforms = new ArrayList<>();
                if (element.has("rotation")) {
                    JsonObject rotation = element.getAsJsonObject("rotation");
                    float angle = (float) Math.toRadians(rotation.get("angle").getAsFloat());
                    Quaternionf quaternion = switch (rotation.get("axis").getAsString()) {
                        case "x" -> new Quaternionf().rotateX(angle);
                        case "y" -> new Quaternionf().rotateY(angle);
                        default -> new Quaternionf().rotateZ(angle);
                    };
                    transforms.add(new Transform(vector(rotation, "origin", 8.0F), quaternion));
                }
                JsonObject faces = element.has("faces") ? element.getAsJsonObject("faces") : new JsonObject();
                for (int face = 0; face < FACES.length; face++) {
                    if (!faces.has(FACES[face])) continue;
                    JsonObject data = faces.getAsJsonObject(FACES[face]);
                    if (!data.has("texture")) continue;
                    String key = resolveTextureKey(textureMap, data.get("texture").getAsString());
                    if (key == null) continue;
                    TextureBatch batch = byKey.get(key);
                    if (batch == null) {
                        if (byKey.size() >= StudioRules.MAX_SCENE_TEXTURES) throw new SceneException("status.nokhframe.invalid_scene_size");
                        batch = TextureBatch.upload(loadJavaTexture(path, key), 16, 16);
                        byKey.put(key, batch);
                        batches.add(batch);
                    }
                    float[] uv = data.has("uv") ? floats(data.getAsJsonArray("uv"), 4) : defaultUv(face, from, to);
                    int rotation = data.has("rotation") ? data.get("rotation").getAsInt() : 0;
                    addFace(batch, face, from, to, uv, 16, 16, rotation, transforms, true);
                }
            }
        };
    }

    private static @Nullable String resolveTextureKey(JsonObject textures, String reference) {
        String value = reference;
        for (int depth = 0; value.startsWith("#") && depth < 16; depth++) {
            String key = value.substring(1);
            if (!textures.has(key)) return null;
            value = textures.get(key).getAsString();
        }
        return value.startsWith("#") ? null : value;
    }

    /** Looks for a PNG next to the model first, then in the game's loaded resource packs. */
    private static NativeImage loadJavaTexture(Path modelPath, String texture) {
        String fileName = texture.substring(texture.lastIndexOf('/') + 1).replace(':', '_');
        Path sibling = modelPath.toAbsolutePath().getParent().resolve(fileName + ".png");
        if (Files.isRegularFile(sibling)) {
            try (InputStream input = Files.newInputStream(sibling)) {
                return NativeImage.read(input);
            } catch (IOException | RuntimeException ignored) {
                // fall through to resource packs
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(texture);
        if (id != null) {
            ResourceLocation file = id.withPath(p -> "textures/" + p + ".png");
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
            if (resource.isPresent()) {
                try (InputStream input = resource.get().open()) {
                    return NativeImage.read(input);
                } catch (IOException | RuntimeException ignored) {
                    // use the missing texture below
                }
            }
        }
        return missingTexture();
    }

    private static float[] defaultUv(int face, Vector3f from, Vector3f to) {
        return switch (FACES[face]) {
            case "down" -> new float[]{from.x, 16 - to.z, to.x, 16 - from.z};
            case "up" -> new float[]{from.x, from.z, to.x, to.z};
            case "north" -> new float[]{16 - to.x, 16 - to.y, 16 - from.x, 16 - from.y};
            case "south" -> new float[]{from.x, 16 - to.y, to.x, 16 - from.y};
            case "west" -> new float[]{from.z, 16 - to.y, to.z, 16 - from.y};
            default -> new float[]{16 - to.z, 16 - to.y, 16 - from.z, 16 - from.y};
        };
    }

    // ---- Geometry -----------------------------------------------------------------------------------------

    /**
     * Adds one cube face. Corners are listed top-left, top-right, bottom-right, bottom-left as seen from outside
     * the face (Minecraft's orientation, "up" faces north), then emitted counter-clockwise.
     */
    private static void addFace(TextureBatch batch, int face, Vector3f from, Vector3f to, float[] uv, float uvWidth,
                                float uvHeight, int rotation, List<Transform> transforms, boolean blockSpace) {
        float x0 = from.x, y0 = from.y, z0 = from.z, x1 = to.x, y1 = to.y, z1 = to.z;
        float[][] corners = switch (FACES[face]) {
            case "north" -> new float[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}};
            case "south" -> new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}};
            case "east" -> new float[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case "west" -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}};
            case "up" -> new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
            default -> new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}};
        };
        Vector3f normal = switch (FACES[face]) {
            case "north" -> new Vector3f(0, 0, -1);
            case "south" -> new Vector3f(0, 0, 1);
            case "east" -> new Vector3f(1, 0, 0);
            case "west" -> new Vector3f(-1, 0, 0);
            case "up" -> new Vector3f(0, 1, 0);
            default -> new Vector3f(0, -1, 0);
        };
        float u0 = uv[0] / uvWidth, v0 = uv[1] / uvHeight, u1 = uv[2] / uvWidth, v1 = uv[3] / uvHeight;
        float[][] uvCorners = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        int steps = Math.floorMod(rotation / 90, 4);
        for (Transform transform : transforms) transform.rotation.transform(normal);

        int[] order = {0, 3, 2, 1};
        for (int corner : order) {
            Vector3f position = new Vector3f(corners[corner][0], corners[corner][1], corners[corner][2]);
            for (Transform transform : transforms) {
                position.sub(transform.origin);
                transform.rotation.transform(position);
                position.add(transform.origin);
            }
            if (blockSpace) position.sub(8.0F, 0.0F, 8.0F);
            position.div(16.0F);
            float[] texture = uvCorners[(corner + 4 - steps) % 4];
            batch.vertices.add(new float[]{position.x, position.y, position.z, texture[0], texture[1],
                    normal.x, normal.y, normal.z});
        }
    }

    private static Quaternionf eulerZyx(Vector3f degrees) {
        return new Quaternionf()
                .rotateZ((float) Math.toRadians(degrees.z))
                .rotateY((float) Math.toRadians(degrees.y))
                .rotateX((float) Math.toRadians(degrees.x));
    }

    private static Vector3f vector(JsonObject object, String key, float fallback) {
        if (!object.has(key) || !object.get(key).isJsonArray()) return new Vector3f(fallback);
        float[] values = floats(object.getAsJsonArray(key), 3);
        return new Vector3f(values[0], values[1], values[2]);
    }

    private static float[] floats(JsonArray array, int count) {
        float[] values = new float[count];
        for (int i = 0; i < count && i < array.size(); i++) values[i] = array.get(i).getAsFloat();
        return values;
    }

    // ---- Textures -----------------------------------------------------------------------------------------

    private static NativeImage decodeDataUri(String source) {
        int comma = source.indexOf(',');
        if (!source.startsWith("data:image/png;base64,") || comma < 0) return missingTexture();
        try {
            byte[] bytes = Base64.getDecoder().decode(source.substring(comma + 1));
            return NativeImage.read(new ByteArrayInputStream(bytes));
        } catch (IOException | IllegalArgumentException exception) {
            return missingTexture();
        }
    }

    private static NativeImage missingTexture() {
        NativeImage image = new NativeImage(2, 2, false);
        image.setPixelRGBA(0, 0, 0xFFFF00FF);
        image.setPixelRGBA(1, 1, 0xFFFF00FF);
        image.setPixelRGBA(1, 0, 0xFF000000);
        image.setPixelRGBA(0, 1, 0xFF000000);
        return image;
    }

    @Override
    public SceneMesh mesh() {
        return mesh;
    }

    @Override
    public void close() {
        mesh.close();
        batches.forEach(TextureBatch::release);
        batches.clear();
    }

    private record Transform(Vector3f origin, Quaternionf rotation) {
    }

    @FunctionalInterface
    private interface Builder {
        void build() throws SceneException;
    }

    private static final class TextureBatch {
        private final ResourceLocation location;
        private final RenderType renderType;
        private final float uvWidth;
        private final float uvHeight;
        private final List<float[]> vertices = new ArrayList<>();

        private TextureBatch(ResourceLocation location, RenderType renderType, float uvWidth, float uvHeight) {
            this.location = location;
            this.renderType = renderType;
            this.uvWidth = uvWidth;
            this.uvHeight = uvHeight;
        }

        static TextureBatch upload(NativeImage image, int uvWidth, int uvHeight) throws SceneException {
            if (!StudioRules.isSupportedBackgroundSize(image.getWidth(), image.getHeight())) {
                image.close();
                throw new SceneException("status.nokhframe.invalid_scene_size");
            }
            boolean translucent = false;
            for (int y = 0; y < image.getHeight() && !translucent; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getPixelRGBA(x, y) >>> 24;
                    if (alpha > 0 && alpha < 255) {
                        translucent = true;
                        break;
                    }
                }
            }
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(NokhFrameMod.MOD_ID,
                    "scene/texture_" + nextTextureId++);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            RenderType renderType = translucent ? RenderType.entityTranslucent(location) : RenderType.entityCutoutNoCull(location);
            return new TextureBatch(location, renderType, Math.max(1, uvWidth), Math.max(1, uvHeight));
        }

        void release() {
            Minecraft.getInstance().getTextureManager().release(location);
        }
    }
}
