package NokhXyr.NokhFrame;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

/** Rules shared by the client screen and headless GameTests. */
public final class StudioRules {
    public static final List<Background> BACKGROUNDS = List.of(
            new Background("green", 0xFF4FC16E),
            new Background("white", 0xFFFFFFFF),
            new Background("gray", 0xFF90969E),
            new Background("blue", 0xFF4A85DB),
            new Background("black", 0xFF20242B)
    );

    private StudioRules() {
    }

    public static final float ATTACK_CYCLE_SECONDS = 1.1F;
    public static final float ATTACK_SWING_SECONDS = 0.3F;

    /** Vanilla swing progress (0 to 1) for a quick main-hand hit followed by a short rest, repeated. */
    public static float attackProgress(float seconds) {
        float phase = seconds % ATTACK_CYCLE_SECONDS;
        return phase < ATTACK_SWING_SECONDS ? phase / ATTACK_SWING_SECONDS : 0.0F;
    }

    public static int nextBackground(int current) {
        return (current + 1) % BACKGROUNDS.size();
    }

    public static int nextSkin(int current, int skinCount) {
        return current + 1 >= skinCount ? -1 : current + 1;
    }

    public static boolean isSkinFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png");
    }

    public static boolean isSupportedSkinSize(int width, int height) {
        return width == 64 && height == 64;
    }

    public static boolean isSupportedBackgroundSize(int width, int height) {
        return width > 0 && height > 0 && width <= 4096 && height <= 4096;
    }

    public static final float MIN_ZOOM = 0.08F;
    public static final float MAX_ZOOM = 3.0F;
    public static final float MIN_PLAYER_SIZE = 0.25F;
    public static final float MAX_PLAYER_SIZE = 4.0F;
    /** Camera distance in blocks for the world view at zoom 1, framing the player like the studio view. */
    public static final float WORLD_CAMERA_DISTANCE = 3.2F;

    public static float clampZoom(float zoom) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    /** Player size steps by 10 % per wheel notch, snapping to exactly 100 % when it passes close by. */
    public static float nextPlayerSize(float size, double notches) {
        float next = (float) (size * Math.pow(1.1, notches));
        if (Math.abs(next - 1.0F) < 0.03F) next = 1.0F;
        return Math.max(MIN_PLAYER_SIZE, Math.min(MAX_PLAYER_SIZE, next));
    }

    public static float worldCameraDistance(float zoom) {
        return WORLD_CAMERA_DISTANCE / clampZoom(zoom);
    }

    public static final long MAX_SCENE_BYTES = 125L * 1024L * 1024L;
    public static final int MAX_STRUCTURE_SIDE = 256;
    public static final int MAX_STRUCTURE_BLOCKS = 2_000_000;
    public static final int MAX_SCENE_ELEMENTS = 200_000;
    public static final int MAX_SCENE_TEXTURES = 256;

    /** 3D sets: structure-block files and Blockbench projects or Java model exports. */
    public static boolean isSceneFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".nbt") || name.endsWith(".bbmodel") || name.endsWith(".json");
    }

    public static boolean isSupportedStructureSize(int x, int y, int z) {
        return x > 0 && y > 0 && z > 0 && x <= MAX_STRUCTURE_SIDE && y <= MAX_STRUCTURE_SIDE && z <= MAX_STRUCTURE_SIDE;
    }

    /**
     * Height where the player stands in a structure's center column: on top of the first solid block that has
     * open space above it, so multi-layer floors work and roofs are ignored. An empty column puts the feet at 0.
     */
    public static int standingHeight(boolean[] solidColumn) {
        for (int y = 0; y < solidColumn.length; y++) {
            if (solidColumn[y] && (y + 1 >= solidColumn.length || !solidColumn[y + 1])) return y + 1;
        }
        return 0;
    }

    public static OptionalInt parseHexColor(String input) {
        String value = input.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (!value.matches("[0-9a-fA-F]{6}")) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(0xFF000000 | Integer.parseInt(value, 16));
    }

    public static boolean matchesItemQuery(String name, String registryId, String query) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        String lowerId = registryId.toLowerCase(Locale.ROOT);
        String namespace = lowerId.substring(0, lowerId.indexOf(':'));
        for (String term : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (term.isEmpty()) continue;
            if (term.startsWith("@")) {
                if (!namespace.contains(term.substring(1))) return false;
            } else if (!lowerName.contains(term) && !lowerId.contains(term)) {
                return false;
            }
        }
        return true;
    }

    public static SkinShape skinShape(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.contains("_slim")) {
            return SkinShape.SLIM;
        }
        if (lower.contains("_wide")) {
            return SkinShape.WIDE;
        }
        return SkinShape.CURRENT;
    }

    public record Background(String name, int color) {
    }

    public enum SkinShape {
        CURRENT, SLIM, WIDE
    }
}
