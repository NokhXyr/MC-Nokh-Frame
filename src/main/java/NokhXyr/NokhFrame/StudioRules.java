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
