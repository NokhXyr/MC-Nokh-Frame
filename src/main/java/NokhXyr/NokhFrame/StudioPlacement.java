package NokhXyr.NokhFrame;

import net.minecraft.util.Mth;

/**
 * Where the player, the 3D scene and the view sit in the studio. Offsets are in blocks along the studio axes:
 * X to the right and Z away from the viewer (as seen from the default front view), Y up. Rotations are degrees
 * around the vertical axis, clockwise seen from above.
 */
public final class StudioPlacement {
    public enum Target { PLAYER, SCENE, VIEW }

    public static final float[] STEPS = {0.0625F, 0.25F, 0.5F, 1.0F};
    public static final float MAX_OFFSET = 64.0F;
    public static final float ROTATION_STEP = 15.0F;
    public static final float MIN_SCENE_SCALE = 0.05F;
    public static final float MAX_SCENE_SCALE = 20.0F;

    public float playerX, playerY, playerZ, playerRotation;
    public float sceneX, sceneY, sceneZ, sceneRotation;
    public float sceneScale = 1.0F;
    /** View pan in blocks: positive X moves the content right, positive Y moves it down. */
    public float panX, panY;

    public static float move(float value, float delta) {
        return Mth.clamp(value + delta, -MAX_OFFSET, MAX_OFFSET);
    }

    public static float rotate(float degrees, float delta) {
        return Mth.wrapDegrees(degrees + delta);
    }

    public static float nextSceneScale(float scale, int direction) {
        float next = (float) (scale * Math.pow(1.1, direction));
        if (Math.abs(next - 1.0F) < 0.03F) next = 1.0F;
        return Mth.clamp(next, MIN_SCENE_SCALE, MAX_SCENE_SCALE);
    }

    public static int nextStep(int stepIndex) {
        return (stepIndex + 1) % STEPS.length;
    }

    public void reset(Target target) {
        switch (target) {
            case PLAYER -> {
                playerX = playerY = playerZ = 0.0F;
                playerRotation = 0.0F;
            }
            case SCENE -> {
                sceneX = sceneY = sceneZ = 0.0F;
                sceneRotation = 0.0F;
                sceneScale = 1.0F;
            }
            case VIEW -> panX = panY = 0.0F;
        }
    }
}
