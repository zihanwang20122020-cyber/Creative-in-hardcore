package dev.zihan.emotestudio.emote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The keyframes belonging to one {@link EmotePart}, kept sorted by time.
 *
 * <p>Sampling is the hot path: it runs once per animated part per rendered player per frame, so it
 * uses a binary search and writes into a caller-owned {@link PartTransform}.
 */
public final class EmoteTrack {
    private final List<Keyframe> keyframes;

    public EmoteTrack() {
        this.keyframes = new ArrayList<>();
    }

    public EmoteTrack(List<Keyframe> keyframes) {
        this.keyframes = new ArrayList<>(keyframes);
        sort();
    }

    public List<Keyframe> keyframes() {
        return Collections.unmodifiableList(keyframes);
    }

    public int size() {
        return keyframes.size();
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public Keyframe get(int index) {
        return keyframes.get(index);
    }

    public float lastTime() {
        return keyframes.isEmpty() ? 0.0F : keyframes.get(keyframes.size() - 1).time();
    }

    private void sort() {
        keyframes.sort(Comparator.comparing(Keyframe::time));
    }

    /**
     * Inserts a keyframe, replacing any existing one at the same time. Returns its index.
     */
    public int put(Keyframe frame) {
        for (int i = 0; i < keyframes.size(); i++) {
            float existing = keyframes.get(i).time();
            if (Math.abs(existing - frame.time()) < 1.0E-4F) {
                keyframes.set(i, frame);
                return i;
            }
            if (existing > frame.time()) {
                keyframes.add(i, frame);
                return i;
            }
        }
        keyframes.add(frame);
        return keyframes.size() - 1;
    }

    public void remove(int index) {
        if (index >= 0 && index < keyframes.size()) {
            keyframes.remove(index);
        }
    }

    public void clear() {
        keyframes.clear();
    }

    /** Index of the keyframe at {@code time}, or -1. */
    public int indexAt(float time) {
        for (int i = 0; i < keyframes.size(); i++) {
            if (Math.abs(keyframes.get(i).time() - time) < 1.0E-4F) {
                return i;
            }
        }
        return -1;
    }

    public EmoteTrack copy() {
        return new EmoteTrack(keyframes);
    }

    /**
     * Writes the pose at {@code time} into {@code out}.
     *
     * @param time   playback time in ticks, already wrapped into {@code 0..length} by the caller
     * @param length total emote length in ticks, used to close the loop
     * @param loop   when true the segment between the final keyframe and {@code length} interpolates
     *               back to the first keyframe so the cycle has no seam
     */
    public void sample(float time, float length, boolean loop, PartTransform out) {
        int count = keyframes.size();
        if (count == 0) {
            out.reset();
            return;
        }
        if (count == 1) {
            out.set(keyframes.get(0));
            return;
        }

        Keyframe first = keyframes.get(0);
        Keyframe last = keyframes.get(count - 1);

        if (time <= first.time()) {
            if (loop && first.time() > 0.0F) {
                // Wrap the tail of the previous cycle into the head of this one.
                float span = (length - last.time()) + first.time();
                if (span <= 1.0E-4F) {
                    out.set(first);
                    return;
                }
                float progress = (time + (length - last.time())) / span;
                out.lerp(last, first, last.easing().applyClamped(progress));
                return;
            }
            out.set(first);
            return;
        }

        if (time >= last.time()) {
            if (loop) {
                float span = (length - last.time()) + first.time();
                if (span <= 1.0E-4F) {
                    out.set(last);
                    return;
                }
                float progress = (time - last.time()) / span;
                out.lerp(last, first, last.easing().applyClamped(progress));
                return;
            }
            out.set(last);
            return;
        }

        int low = 0;
        int high = count - 1;
        while (high - low > 1) {
            int mid = (low + high) >>> 1;
            if (keyframes.get(mid).time() <= time) {
                low = mid;
            } else {
                high = mid;
            }
        }

        Keyframe from = keyframes.get(low);
        Keyframe to = keyframes.get(high);
        float span = to.time() - from.time();
        if (span <= 1.0E-4F) {
            out.set(to);
            return;
        }
        out.lerp(from, to, from.easing().applyClamped((time - from.time()) / span));
    }
}
