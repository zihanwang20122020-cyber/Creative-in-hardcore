package dev.zihan.emotestudio.emote;

/**
 * One pose of one {@link EmotePart} at one point in time.
 *
 * <p>Rotations are stored in <em>degrees</em> and translations in <em>model pixels</em> (1/16 of a
 * block) because those are the units the in-game editor shows and the units emote files are written
 * in. Conversion to radians and blocks happens once, at apply time.
 *
 * <p>{@code easing} describes how to travel <em>away from</em> this keyframe towards the next one.
 */
public record Keyframe(
        float time,
        float rotX, float rotY, float rotZ,
        float posX, float posY, float posZ,
        float scaleX, float scaleY, float scaleZ,
        Easing easing) {

    public static final Keyframe IDENTITY = new Keyframe(0.0F, 0, 0, 0, 0, 0, 0, 1, 1, 1, Easing.LINEAR);

    public Keyframe {
        if (easing == null) {
            easing = Easing.LINEAR;
        }
        if (!Float.isFinite(time) || time < 0.0F) {
            time = 0.0F;
        }
    }

    public static Keyframe at(float time) {
        return new Keyframe(time, 0, 0, 0, 0, 0, 0, 1, 1, 1, Easing.LINEAR);
    }

    /** Rotation-only keyframe, the shape the vast majority of emote keyframes take. */
    public static Keyframe rot(float time, float rotX, float rotY, float rotZ, Easing easing) {
        return new Keyframe(time, rotX, rotY, rotZ, 0, 0, 0, 1, 1, 1, easing);
    }

    public Keyframe withTime(float newTime) {
        return new Keyframe(newTime, rotX, rotY, rotZ, posX, posY, posZ, scaleX, scaleY, scaleZ, easing);
    }

    public Keyframe withEasing(Easing newEasing) {
        return new Keyframe(time, rotX, rotY, rotZ, posX, posY, posZ, scaleX, scaleY, scaleZ, newEasing);
    }

    public Keyframe withRotation(float x, float y, float z) {
        return new Keyframe(time, x, y, z, posX, posY, posZ, scaleX, scaleY, scaleZ, easing);
    }

    public Keyframe withPosition(float x, float y, float z) {
        return new Keyframe(time, rotX, rotY, rotZ, x, y, z, scaleX, scaleY, scaleZ, easing);
    }

    public Keyframe withScale(float x, float y, float z) {
        return new Keyframe(time, rotX, rotY, rotZ, posX, posY, posZ, x, y, z, easing);
    }

    /** Reads one of the nine animatable channels by index, used by the editor's generic sliders. */
    public float channel(int index) {
        return switch (index) {
            case 0 -> rotX;
            case 1 -> rotY;
            case 2 -> rotZ;
            case 3 -> posX;
            case 4 -> posY;
            case 5 -> posZ;
            case 6 -> scaleX;
            case 7 -> scaleY;
            case 8 -> scaleZ;
            default -> 0.0F;
        };
    }

    public Keyframe withChannel(int index, float value) {
        return switch (index) {
            case 0 -> withRotation(value, rotY, rotZ);
            case 1 -> withRotation(rotX, value, rotZ);
            case 2 -> withRotation(rotX, rotY, value);
            case 3 -> withPosition(value, posY, posZ);
            case 4 -> withPosition(posX, value, posZ);
            case 5 -> withPosition(posX, posY, value);
            case 6 -> withScale(value, scaleY, scaleZ);
            case 7 -> withScale(scaleX, value, scaleZ);
            case 8 -> withScale(scaleX, scaleY, value);
            default -> this;
        };
    }

    public boolean isNeutral() {
        return rotX == 0 && rotY == 0 && rotZ == 0
                && posX == 0 && posY == 0 && posZ == 0
                && scaleX == 1 && scaleY == 1 && scaleZ == 1;
    }
}
