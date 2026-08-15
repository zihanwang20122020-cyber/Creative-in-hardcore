package dev.zihan.emotestudio.emote;

import java.util.Locale;

/**
 * Interpolation curves usable between two keyframes.
 *
 * <p>Every curve maps a normalised progress {@code 0..1} onto an eased progress. Most curves stay
 * inside {@code 0..1}, but the {@code BACK} and {@code ELASTIC} families deliberately overshoot,
 * which is what gives snappy emotes their punch.
 */
public enum Easing {
    /** Holds the start value until the next keyframe is reached. */
    STEP {
        @Override
        public float apply(float t) {
            return t >= 1.0F ? 1.0F : 0.0F;
        }
    },
    LINEAR {
        @Override
        public float apply(float t) {
            return t;
        }
    },
    SMOOTH {
        @Override
        public float apply(float t) {
            return t * t * (3.0F - 2.0F * t);
        }
    },
    SMOOTHER {
        @Override
        public float apply(float t) {
            return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
        }
    },
    SINE_IN {
        @Override
        public float apply(float t) {
            return 1.0F - (float) Math.cos((t * Math.PI) / 2.0);
        }
    },
    SINE_OUT {
        @Override
        public float apply(float t) {
            return (float) Math.sin((t * Math.PI) / 2.0);
        }
    },
    SINE_IN_OUT {
        @Override
        public float apply(float t) {
            return (float) (-(Math.cos(Math.PI * t) - 1.0) / 2.0);
        }
    },
    QUAD_IN {
        @Override
        public float apply(float t) {
            return t * t;
        }
    },
    QUAD_OUT {
        @Override
        public float apply(float t) {
            return 1.0F - (1.0F - t) * (1.0F - t);
        }
    },
    QUAD_IN_OUT {
        @Override
        public float apply(float t) {
            return t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0 * t + 2.0, 2.0) / 2.0F;
        }
    },
    CUBIC_IN {
        @Override
        public float apply(float t) {
            return t * t * t;
        }
    },
    CUBIC_OUT {
        @Override
        public float apply(float t) {
            return 1.0F - (float) Math.pow(1.0 - t, 3.0);
        }
    },
    CUBIC_IN_OUT {
        @Override
        public float apply(float t) {
            return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float) Math.pow(-2.0 * t + 2.0, 3.0) / 2.0F;
        }
    },
    QUART_IN {
        @Override
        public float apply(float t) {
            return t * t * t * t;
        }
    },
    QUART_OUT {
        @Override
        public float apply(float t) {
            return 1.0F - (float) Math.pow(1.0 - t, 4.0);
        }
    },
    QUART_IN_OUT {
        @Override
        public float apply(float t) {
            return t < 0.5F ? 8.0F * t * t * t * t : 1.0F - (float) Math.pow(-2.0 * t + 2.0, 4.0) / 2.0F;
        }
    },
    EXPO_IN {
        @Override
        public float apply(float t) {
            return t <= 0.0F ? 0.0F : (float) Math.pow(2.0, 10.0 * t - 10.0);
        }
    },
    EXPO_OUT {
        @Override
        public float apply(float t) {
            return t >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0 * t);
        }
    },
    EXPO_IN_OUT {
        @Override
        public float apply(float t) {
            if (t <= 0.0F) {
                return 0.0F;
            }
            if (t >= 1.0F) {
                return 1.0F;
            }
            return t < 0.5F
                    ? (float) Math.pow(2.0, 20.0 * t - 10.0) / 2.0F
                    : (2.0F - (float) Math.pow(2.0, -20.0 * t + 10.0)) / 2.0F;
        }
    },
    CIRC_IN {
        @Override
        public float apply(float t) {
            return 1.0F - (float) Math.sqrt(1.0 - t * t);
        }
    },
    CIRC_OUT {
        @Override
        public float apply(float t) {
            return (float) Math.sqrt(1.0 - (t - 1.0) * (t - 1.0));
        }
    },
    CIRC_IN_OUT {
        @Override
        public float apply(float t) {
            return t < 0.5F
                    ? (1.0F - (float) Math.sqrt(1.0 - 4.0 * t * t)) / 2.0F
                    : ((float) Math.sqrt(1.0 - Math.pow(-2.0 * t + 2.0, 2.0)) + 1.0F) / 2.0F;
        }
    },
    BACK_IN {
        @Override
        public float apply(float t) {
            return BACK_C3 * t * t * t - BACK_C1 * t * t;
        }
    },
    BACK_OUT {
        @Override
        public float apply(float t) {
            float u = t - 1.0F;
            return 1.0F + BACK_C3 * u * u * u + BACK_C1 * u * u;
        }
    },
    BACK_IN_OUT {
        @Override
        public float apply(float t) {
            float c2 = BACK_C1 * 1.525F;
            return t < 0.5F
                    ? ((float) Math.pow(2.0 * t, 2.0) * ((c2 + 1.0F) * 2.0F * t - c2)) / 2.0F
                    : ((float) Math.pow(2.0 * t - 2.0, 2.0) * ((c2 + 1.0F) * (t * 2.0F - 2.0F) + c2) + 2.0F) / 2.0F;
        }
    },
    ELASTIC_IN {
        @Override
        public float apply(float t) {
            if (t <= 0.0F) {
                return 0.0F;
            }
            if (t >= 1.0F) {
                return 1.0F;
            }
            return (float) (-Math.pow(2.0, 10.0 * t - 10.0) * Math.sin((t * 10.0 - 10.75) * ELASTIC_C4));
        }
    },
    ELASTIC_OUT {
        @Override
        public float apply(float t) {
            if (t <= 0.0F) {
                return 0.0F;
            }
            if (t >= 1.0F) {
                return 1.0F;
            }
            return (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * ELASTIC_C4) + 1.0);
        }
    },
    ELASTIC_IN_OUT {
        @Override
        public float apply(float t) {
            if (t <= 0.0F) {
                return 0.0F;
            }
            if (t >= 1.0F) {
                return 1.0F;
            }
            double c5 = (2.0 * Math.PI) / 4.5;
            return t < 0.5F
                    ? (float) (-(Math.pow(2.0, 20.0 * t - 10.0) * Math.sin((20.0 * t - 11.125) * c5)) / 2.0)
                    : (float) ((Math.pow(2.0, -20.0 * t + 10.0) * Math.sin((20.0 * t - 11.125) * c5)) / 2.0 + 1.0);
        }
    },
    BOUNCE_IN {
        @Override
        public float apply(float t) {
            return 1.0F - bounceOut(1.0F - t);
        }
    },
    BOUNCE_OUT {
        @Override
        public float apply(float t) {
            return bounceOut(t);
        }
    },
    BOUNCE_IN_OUT {
        @Override
        public float apply(float t) {
            return t < 0.5F
                    ? (1.0F - bounceOut(1.0F - 2.0F * t)) / 2.0F
                    : (1.0F + bounceOut(2.0F * t - 1.0F)) / 2.0F;
        }
    };

    private static final float BACK_C1 = 1.70158F;
    private static final float BACK_C3 = BACK_C1 + 1.0F;
    private static final double ELASTIC_C4 = (2.0 * Math.PI) / 3.0;

    public abstract float apply(float t);

    /**
     * Clamps {@code t} into {@code 0..1} before easing, so callers never have to bounds-check.
     */
    public float applyClamped(float t) {
        if (t <= 0.0F) {
            return 0.0F;
        }
        if (t >= 1.0F) {
            return 1.0F;
        }
        return apply(t);
    }

    private static float bounceOut(float t) {
        float n1 = 7.5625F;
        float d1 = 2.75F;
        if (t < 1.0F / d1) {
            return n1 * t * t;
        }
        if (t < 2.0F / d1) {
            float u = t - 1.5F / d1;
            return n1 * u * u + 0.75F;
        }
        if (t < 2.5F / d1) {
            float u = t - 2.25F / d1;
            return n1 * u * u + 0.9375F;
        }
        float u = t - 2.625F / d1;
        return n1 * u * u + 0.984375F;
    }

    /**
     * Parses an easing name from an emote file. Unknown or missing names fall back to {@link #LINEAR}
     * so a typo degrades one keyframe instead of rejecting the whole emote.
     */
    public static Easing byName(String name) {
        if (name == null || name.isBlank()) {
            return LINEAR;
        }
        String key = name.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (Easing easing : values()) {
            if (easing.name().equals(key)) {
                return easing;
            }
        }
        return switch (key) {
            case "EASE", "EASE_IN_OUT", "SMOOTHSTEP" -> SMOOTH;
            case "EASE_IN" -> SINE_IN;
            case "EASE_OUT" -> SINE_OUT;
            case "CONSTANT", "HOLD", "NONE" -> STEP;
            default -> LINEAR;
        };
    }

    public String serialisedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
