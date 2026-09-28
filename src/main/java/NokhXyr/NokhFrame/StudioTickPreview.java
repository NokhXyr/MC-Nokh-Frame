package NokhXyr.NokhFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Client-only values exposed while other mods process a world or client tick. */
public final class StudioTickPreview {
    private static boolean active;
    private static LocalPlayer player;
    private static ItemStack item = ItemStack.EMPTY;
    private static ItemEntity previewEntity;
    private static StudioAvatarRenderer.Motion motion = StudioAvatarRenderer.Motion.IDLE;
    private static Vec3 movementOffset = Vec3.ZERO;

    private StudioTickPreview() {
    }

    public static void begin() {
        Minecraft minecraft = Minecraft.getInstance();
        clear();
        if (!(minecraft.screen instanceof StudioScreen studio) || minecraft.player == null) return;
        player = minecraft.player;
        if (studio.previewingItem()) {
            item = studio.previewItem().copy();
            if (!item.isEmpty() && minecraft.level != null) {
                Vec3 center = minecraft.player.position().add(0.0, minecraft.player.getBbHeight() * 0.5, 0.0);
                previewEntity = new ItemEntity(minecraft.level, center.x, center.y, center.z, item.copy());
            }
        } else {
            motion = studio.previewMotion();
            if (motion == StudioAvatarRenderer.Motion.WALK || motion == StudioAvatarRenderer.Motion.RUN
                    || motion == StudioAvatarRenderer.Motion.SNEAK) {
                double phase = minecraft.level == null ? 0.0 : minecraft.level.getGameTime() * Math.PI / 16.0;
                double radius = motion == StudioAvatarRenderer.Motion.RUN ? 0.8 : 0.45;
                movementOffset = new Vec3(Math.cos(phase) * radius, 0.0, Math.sin(phase) * radius);
            }
        }
        active = !item.isEmpty() || motion == StudioAvatarRenderer.Motion.WALK
                || motion == StudioAvatarRenderer.Motion.RUN || motion == StudioAvatarRenderer.Motion.SNEAK;
    }

    public static void clear() {
        active = false;
        player = null;
        item = ItemStack.EMPTY;
        previewEntity = null;
        motion = StudioAvatarRenderer.Motion.IDLE;
        movementOffset = Vec3.ZERO;
    }

    public static boolean appliesTo(Object entity) {
        return active && entity == player;
    }

    public static boolean simulatesMovementFor(Object entity) {
        return appliesTo(entity) && item.isEmpty();
    }

    public static ItemStack previewHeldItem() {
        return item;
    }

    public static ItemEntity previewItemEntity() {
        return active ? previewEntity : null;
    }

    public static boolean isCrouching() {
        return motion == StudioAvatarRenderer.Motion.SNEAK;
    }

    public static boolean isRunning() {
        return motion == StudioAvatarRenderer.Motion.RUN;
    }

    public static Vec3 simulatedDelta() {
        return StudioAvatarRenderer.simulatedDelta(motion);
    }

    public static Vec3 offsetPosition(Vec3 actual) {
        return actual.add(movementOffset);
    }
}
