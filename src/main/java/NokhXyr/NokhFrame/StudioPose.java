package NokhXyr.NokhFrame;

import net.minecraft.util.Mth;

/**
 * Player model pose for the studio, like an armor stand: X/Y/Z rotations in degrees for each body part.
 * Arms follow the armor stand convention (positive Z raises the right arm outward, negative Z the left arm).
 * The {@link Preset#ANIMATION} preset leaves the model to the selected motion.
 */
public final class StudioPose {
    public enum Part { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

    public enum Preset {
        ANIMATION(null),
        NEUTRAL(new float[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}}),
        SIT(new float[][]{{0, 0, 0}, {0, 0, 0}, {-36, 0, 0}, {-36, 0, 0}, {-81, 18, 4.5F}, {-81, -18, -4.5F}}),
        WAVE(new float[][]{{0, 0, 5}, {0, 0, 0}, {0, 0, 150}, {0, 0, -8}, {0, 0, 0}, {0, 0, 0}}),
        T_POSE(new float[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 90}, {0, 0, -90}, {0, 0, 0}, {0, 0, 0}}),
        VICTORY(new float[][]{{-15, 0, 0}, {0, 0, 0}, {-10, 0, 160}, {-10, 0, -160}, {0, 0, 3}, {0, 0, -3}}),
        POINT(new float[][]{{0, -10, 0}, {0, 0, 0}, {-90, 0, 0}, {0, 0, -8}, {0, 0, 0}, {0, 0, 0}}),
        THINKER(new float[][]{{15, 0, 0}, {0, 0, 0}, {-125, -35, 0}, {-30, 20, 0}, {0, 0, 0}, {0, 0, 0}}),
        PROUD(new float[][]{{-10, 0, 0}, {0, 0, 0}, {10, 0, 25}, {10, 0, -25}, {0, 0, 4}, {0, 0, -4}}),
        RUN(new float[][]{{-5, 0, 0}, {0, 0, 0}, {60, 0, 5}, {-60, 0, -5}, {-45, 0, 0}, {45, 0, 0}}),
        CUSTOM(null);

        private final float[][] angles;

        Preset(float[][] angles) {
            this.angles = angles;
        }
    }

    public static final float MAX_ANGLE = 180.0F;

    private Preset preset = Preset.ANIMATION;
    private final float[][] angles = new float[Part.values().length][3];

    public Preset preset() {
        return preset;
    }

    /** True when the pose drives the model instead of the motion animation. */
    public boolean active() {
        return preset != Preset.ANIMATION;
    }

    public void apply(Preset next) {
        preset = next;
        if (next.angles != null) {
            for (int part = 0; part < angles.length; part++) angles[part] = next.angles[part].clone();
        }
    }

    public Preset nextPreset() {
        Preset[] presets = Preset.values();
        Preset next = presets[(preset.ordinal() + 1) % presets.length];
        // Custom is reached by editing a part, not by cycling.
        if (next == Preset.CUSTOM) next = presets[0];
        apply(next);
        return next;
    }

    public float angle(Part part, int axis) {
        return angles[part.ordinal()][axis];
    }

    /** Edits one axis; any edit turns the pose into a custom pose that starts from the current angles. */
    public void adjust(Part part, int axis, float delta) {
        if (!active()) apply(Preset.NEUTRAL);
        angles[part.ordinal()][axis] = Mth.clamp(angles[part.ordinal()][axis] + delta, -MAX_ANGLE, MAX_ANGLE);
        preset = Preset.CUSTOM;
    }

    /** Copies an arm or leg onto the opposite side, mirrored (Y and Z flip sign). The head and body are symmetric. */
    public void mirror(Part part) {
        Part opposite = switch (part) {
            case RIGHT_ARM -> Part.LEFT_ARM;
            case LEFT_ARM -> Part.RIGHT_ARM;
            case RIGHT_LEG -> Part.LEFT_LEG;
            case LEFT_LEG -> Part.RIGHT_LEG;
            default -> null;
        };
        if (opposite == null) return;
        if (!active()) apply(Preset.NEUTRAL);
        float[] source = angles[part.ordinal()];
        angles[opposite.ordinal()] = new float[]{source[0], -source[1], -source[2]};
        preset = Preset.CUSTOM;
    }

    public void resetPart(Part part) {
        if (!active()) return;
        angles[part.ordinal()] = new float[3];
        preset = Preset.CUSTOM;
    }

    public static float radians(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }
}
