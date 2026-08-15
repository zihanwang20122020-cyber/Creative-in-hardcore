package dev.zihan.emotestudio.emote;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A complete emote: metadata plus one {@link EmoteTrack} per animated part.
 *
 * <p>Instances are mutable so the in-game editor can work on them directly; anything handed to the
 * renderer is either a built-in (never edited in place) or an explicit {@link #copy()}.
 */
public final class Emote {
    /** Where an emote came from, which decides whether it can be overwritten on disk. */
    public enum Source {
        /** Shipped inside the mod jar. */
        BUILTIN,
        /** Loaded from the user's emotes folder. */
        USER,
        /** Received over the network from another player, kept only for this session. */
        REMOTE
    }

    private String id;
    private String name;
    private String author;
    private String description;
    private String category;
    private final List<String> tags = new ArrayList<>();

    private float lengthTicks = 40.0F;
    private boolean loop = true;
    private float blendInTicks = 3.0F;
    private float blendOutTicks = 3.0F;
    private boolean stopOnMove;
    private float speed = 1.0F;

    private final Map<EmotePart, EmoteTrack> tracks = new EnumMap<>(EmotePart.class);

    private Source source = Source.USER;
    private Path file;

    public Emote(String id, String name) {
        this.id = sanitiseId(id);
        this.name = name == null || name.isBlank() ? this.id : name;
        this.author = "";
        this.description = "";
        this.category = "custom";
    }

    public static String sanitiseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "emote";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (char c : raw.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                builder.append(c);
            } else if (c == ' ') {
                builder.append('_');
            }
        }
        String result = builder.toString();
        return result.isBlank() ? "emote" : result;
    }

    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = sanitiseId(id);
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String author() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author == null ? "" : author;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public String category() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null || category.isBlank() ? "custom" : category.toLowerCase(Locale.ROOT);
    }

    public List<String> tags() {
        return tags;
    }

    public float lengthTicks() {
        return lengthTicks;
    }

    public void setLengthTicks(float lengthTicks) {
        this.lengthTicks = Math.max(1.0F, lengthTicks);
    }

    public boolean loop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public float blendInTicks() {
        return blendInTicks;
    }

    public void setBlendInTicks(float blendInTicks) {
        this.blendInTicks = Math.max(0.0F, blendInTicks);
    }

    public float blendOutTicks() {
        return blendOutTicks;
    }

    public void setBlendOutTicks(float blendOutTicks) {
        this.blendOutTicks = Math.max(0.0F, blendOutTicks);
    }

    public boolean stopOnMove() {
        return stopOnMove;
    }

    public void setStopOnMove(boolean stopOnMove) {
        this.stopOnMove = stopOnMove;
    }

    public float speed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed <= 0.0F ? 1.0F : Math.min(speed, 8.0F);
    }

    public Source source() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Path file() {
        return file;
    }

    public void setFile(Path file) {
        this.file = file;
    }

    public boolean editable() {
        return source == Source.USER;
    }

    public Map<EmotePart, EmoteTrack> tracks() {
        return tracks;
    }

    public EmoteTrack track(EmotePart part) {
        return tracks.get(part);
    }

    /** Returns the track for {@code part}, creating an empty one if the emote had none. */
    public EmoteTrack trackOrCreate(EmotePart part) {
        return tracks.computeIfAbsent(part, ignored -> new EmoteTrack());
    }

    public void setTrack(EmotePart part, EmoteTrack track) {
        if (track == null || track.isEmpty()) {
            tracks.remove(part);
        } else {
            tracks.put(part, track);
        }
    }

    public boolean isAnimated(EmotePart part) {
        EmoteTrack track = tracks.get(part);
        return track != null && !track.isEmpty();
    }

    public int keyframeCount() {
        int total = 0;
        for (EmoteTrack track : tracks.values()) {
            total += track.size();
        }
        return total;
    }

    /**
     * Samples the whole emote at {@code time} ticks into {@code out}.
     *
     * @param time playback time in ticks; wrapped when the emote loops, clamped otherwise
     */
    public void sample(float time, EmotePose out) {
        float t = time;
        if (loop) {
            t = t % lengthTicks;
            if (t < 0.0F) {
                t += lengthTicks;
            }
        } else {
            t = Math.clamp(t, 0.0F, lengthTicks);
        }
        for (EmotePart part : EmotePart.values()) {
            PartTransform transform = out.get(part);
            EmoteTrack track = tracks.get(part);
            if (track == null || track.isEmpty()) {
                transform.reset();
            } else {
                track.sample(t, lengthTicks, loop, transform);
            }
        }
    }

    /**
     * Blend weight at {@code time}, covering both the fade-in at the start and, for non-looping
     * emotes, the fade-out at the end.
     */
    public float blendWeight(float time) {
        float weight = 1.0F;
        if (blendInTicks > 0.0F && time < blendInTicks) {
            weight = Math.min(weight, Math.clamp(time / blendInTicks, 0.0F, 1.0F));
        }
        if (!loop && blendOutTicks > 0.0F) {
            float remaining = lengthTicks - time;
            if (remaining < blendOutTicks) {
                weight = Math.min(weight, Math.clamp(remaining / blendOutTicks, 0.0F, 1.0F));
            }
        }
        return weight;
    }

    public Emote copy() {
        Emote clone = new Emote(id, name);
        clone.author = author;
        clone.description = description;
        clone.category = category;
        clone.tags.addAll(tags);
        clone.lengthTicks = lengthTicks;
        clone.loop = loop;
        clone.blendInTicks = blendInTicks;
        clone.blendOutTicks = blendOutTicks;
        clone.stopOnMove = stopOnMove;
        clone.speed = speed;
        clone.source = source;
        clone.file = file;
        for (Map.Entry<EmotePart, EmoteTrack> entry : tracks.entrySet()) {
            clone.tracks.put(entry.getKey(), entry.getValue().copy());
        }
        return clone;
    }

    /** Match used by the search boxes in the browser and the wheel. */
    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (id.contains(q) || name.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (category.contains(q) || author.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (String tag : tags) {
            if (tag.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return description.toLowerCase(Locale.ROOT).contains(q);
    }

    @Override
    public String toString() {
        return "Emote[" + id + ", " + keyframeCount() + " keyframes, " + lengthTicks + " ticks]";
    }
}
