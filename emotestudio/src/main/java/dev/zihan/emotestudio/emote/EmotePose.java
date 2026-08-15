package dev.zihan.emotestudio.emote;

/** A full sampled avatar pose: one {@link PartTransform} per {@link EmotePart}. */
public final class EmotePose {
    private final PartTransform[] transforms = new PartTransform[EmotePart.values().length];

    public EmotePose() {
        for (int i = 0; i < transforms.length; i++) {
            transforms[i] = new PartTransform();
        }
    }

    public PartTransform get(EmotePart part) {
        return transforms[part.ordinal()];
    }

    public void reset() {
        for (PartTransform transform : transforms) {
            transform.reset();
        }
    }
}
