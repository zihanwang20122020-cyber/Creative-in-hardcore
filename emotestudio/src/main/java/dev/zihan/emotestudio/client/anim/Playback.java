package dev.zihan.emotestudio.client.anim;

import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.EmotePose;

/**
 * One player's in-flight emote.
 *
 * <p>Time is measured in client ticks with sub-tick precision, so an emote stays smooth at any
 * framerate. A stopped emote is not dropped immediately: it fades back to the normal pose over
 * {@link Emote#blendOutTicks()} so nobody snaps.
 */
public final class Playback {
    private final Emote emote;
    private final float startTick;
    private final float speed;

    private float stopTick = Float.NaN;

    public Playback(Emote emote, float startTick, float speed) {
        this.emote = emote;
        this.startTick = startTick;
        this.speed = !Float.isFinite(speed) || speed <= 0.0F ? 1.0F : Math.min(speed, 8.0F);
    }

    public Emote emote() {
        return emote;
    }

    public float speed() {
        return speed;
    }

    public boolean stopping() {
        return !Float.isNaN(stopTick);
    }

    /** Begins the fade-out. Calling it twice keeps the first stop time. */
    public void requestStop(float now) {
        if (Float.isNaN(stopTick)) {
            stopTick = now;
        }
    }

    /** Emote-local time in ticks at wall-clock time {@code now}. */
    public float emoteTime(float now) {
        float elapsed = (now - startTick) * speed * emote.speed();
        return Math.max(0.0F, elapsed);
    }

    /**
     * How strongly the emote pose should override the vanilla pose, from 0 (vanilla) to 1 (emote).
     */
    public float weight(float now) {
        float time = emoteTime(now);
        float weight = emote.blendWeight(time);
        if (stopping()) {
            float fade = Math.max(emote.blendOutTicks(), 1.0F);
            float since = (now - stopTick) * speed;
            weight = Math.min(weight, Math.clamp(1.0F - since / fade, 0.0F, 1.0F));
        }
        return weight;
    }

    /** True once the emote has run out and faded away, so the manager can forget it. */
    public boolean finished(float now) {
        if (stopping()) {
            float fade = Math.max(emote.blendOutTicks(), 1.0F);
            return (now - stopTick) * speed >= fade;
        }
        if (emote.loop()) {
            return false;
        }
        return emoteTime(now) >= emote.lengthTicks();
    }

    public void sample(float now, EmotePose out) {
        emote.sample(emoteTime(now), out);
    }
}
