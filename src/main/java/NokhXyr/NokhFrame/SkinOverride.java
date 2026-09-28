package NokhXyr.NokhFrame;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Restricts the replacement skin to the studio avatar render call. */
public final class SkinOverride {
    private static boolean renderingAvatar;
    private static @Nullable PlayerSkin skin;

    private SkinOverride() {
    }

    public static void begin(@Nullable ResourceLocation texture, PlayerSkin original, PlayerSkin.Model model) {
        skin = texture == null ? null : new PlayerSkin(
                texture,
                null,
                original.capeTexture(),
                original.elytraTexture(),
                model,
                false
        );
        renderingAvatar = true;
    }

    public static void end() {
        renderingAvatar = false;
    }

    public static @Nullable PlayerSkin activeSkin() {
        return renderingAvatar ? skin : null;
    }

    public static boolean isRenderingAvatar() {
        return renderingAvatar;
    }
}
