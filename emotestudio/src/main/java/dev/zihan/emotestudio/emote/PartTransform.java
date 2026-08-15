package dev.zihan.emotestudio.emote;

/**
 * Mutable sampled pose of a single part. Reused between frames so sampling an emote allocates
 * nothing on the render path.
 */
public final class PartTransform {
    public float rotX;
    public float rotY;
    public float rotZ;
    public float posX;
    public float posY;
    public float posZ;
    public float scaleX = 1.0F;
    public float scaleY = 1.0F;
    public float scaleZ = 1.0F;

    /** True when this part carries no animation and can be skipped entirely. */
    public boolean animated;

    public void reset() {
        rotX = 0.0F;
        rotY = 0.0F;
        rotZ = 0.0F;
        posX = 0.0F;
        posY = 0.0F;
        posZ = 0.0F;
        scaleX = 1.0F;
        scaleY = 1.0F;
        scaleZ = 1.0F;
        animated = false;
    }

    public void set(Keyframe frame) {
        rotX = frame.rotX();
        rotY = frame.rotY();
        rotZ = frame.rotZ();
        posX = frame.posX();
        posY = frame.posY();
        posZ = frame.posZ();
        scaleX = frame.scaleX();
        scaleY = frame.scaleY();
        scaleZ = frame.scaleZ();
        animated = true;
    }

    public void lerp(Keyframe from, Keyframe to, float t) {
        rotX = from.rotX() + (to.rotX() - from.rotX()) * t;
        rotY = from.rotY() + (to.rotY() - from.rotY()) * t;
        rotZ = from.rotZ() + (to.rotZ() - from.rotZ()) * t;
        posX = from.posX() + (to.posX() - from.posX()) * t;
        posY = from.posY() + (to.posY() - from.posY()) * t;
        posZ = from.posZ() + (to.posZ() - from.posZ()) * t;
        scaleX = from.scaleX() + (to.scaleX() - from.scaleX()) * t;
        scaleY = from.scaleY() + (to.scaleY() - from.scaleY()) * t;
        scaleZ = from.scaleZ() + (to.scaleZ() - from.scaleZ()) * t;
        animated = true;
    }

    public Keyframe toKeyframe(float time, Easing easing) {
        return new Keyframe(time, rotX, rotY, rotZ, posX, posY, posZ, scaleX, scaleY, scaleZ, easing);
    }
}
