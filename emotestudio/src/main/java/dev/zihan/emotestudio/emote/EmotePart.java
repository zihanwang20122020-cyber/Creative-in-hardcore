package dev.zihan.emotestudio.emote;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The animatable pieces of a player. {@link #ROOT} is not a model part: it offsets and spins the
 * whole avatar in world space, which is what makes emotes like {@code float} or {@code moonwalk}
 * read as movement rather than limb flailing.
 */
public enum EmotePart {
    ROOT("root", "Root"),
    HEAD("head", "Head"),
    BODY("body", "Torso"),
    RIGHT_ARM("rightArm", "Right arm"),
    LEFT_ARM("leftArm", "Left arm"),
    RIGHT_LEG("rightLeg", "Right leg"),
    LEFT_LEG("leftLeg", "Left leg");

    /** Parts that map onto a real {@code ModelPart}, i.e. everything except {@link #ROOT}. */
    public static final EmotePart[] MODEL_PARTS = {HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG};

    private static final Map<String, EmotePart> BY_KEY = Stream.of(values())
            .collect(Collectors.toMap(part -> part.key.toLowerCase(Locale.ROOT), part -> part));

    private final String key;
    private final String displayName;

    EmotePart(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isRoot() {
        return this == ROOT;
    }

    public static EmotePart byKey(String key) {
        if (key == null) {
            return null;
        }
        String normalised = key.trim().toLowerCase(Locale.ROOT).replace("_", "");
        EmotePart direct = BY_KEY.get(key.trim().toLowerCase(Locale.ROOT));
        if (direct != null) {
            return direct;
        }
        return switch (normalised) {
            case "rightarm", "armright", "rarm" -> RIGHT_ARM;
            case "leftarm", "armleft", "larm" -> LEFT_ARM;
            case "rightleg", "legright", "rleg" -> RIGHT_LEG;
            case "leftleg", "legleft", "lleg" -> LEFT_LEG;
            case "torso", "chest" -> BODY;
            case "whole", "entity", "world" -> ROOT;
            default -> null;
        };
    }
}
