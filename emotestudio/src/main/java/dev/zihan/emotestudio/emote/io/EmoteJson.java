package dev.zihan.emotestudio.emote.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.zihan.emotestudio.emote.Easing;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.EmotePart;
import dev.zihan.emotestudio.emote.EmoteTrack;
import dev.zihan.emotestudio.emote.Keyframe;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the {@code .emote.json} format.
 *
 * <p>The reader is deliberately forgiving: a malformed keyframe is skipped rather than failing the
 * file, because a hand-edited emote losing one frame beats losing the whole emote. Structural
 * problems (no id, no tracks at all) still raise {@link EmoteParseException}.
 */
public final class EmoteJson {
    public static final int FORMAT_VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private EmoteJson() {
    }

    /** Thrown when a file cannot be understood as an emote at all. */
    public static final class EmoteParseException extends RuntimeException {
        public EmoteParseException(String message) {
            super(message);
        }
    }

    public static Emote read(Reader reader, String fallbackId) {
        JsonElement root = JsonParser.parseReader(reader);
        if (root == null || !root.isJsonObject()) {
            throw new EmoteParseException("Emote file is not a JSON object");
        }
        return read(root.getAsJsonObject(), fallbackId);
    }

    public static Emote read(JsonObject json, String fallbackId) {
        String id = string(json, "id", fallbackId);
        if (id == null || id.isBlank()) {
            throw new EmoteParseException("Emote has no id");
        }
        String name = string(json, "name", id);
        Emote emote = new Emote(id, name);
        emote.setAuthor(string(json, "author", ""));
        emote.setDescription(string(json, "description", ""));
        emote.setCategory(string(json, "category", "custom"));
        emote.setLengthTicks(number(json, "length", 40.0F));
        emote.setLoop(bool(json, "loop", true));
        emote.setBlendInTicks(number(json, "blendIn", 3.0F));
        emote.setBlendOutTicks(number(json, "blendOut", 3.0F));
        emote.setStopOnMove(bool(json, "stopOnMove", false));
        emote.setSpeed(number(json, "speed", 1.0F));

        if (json.has("tags") && json.get("tags").isJsonArray()) {
            for (JsonElement tag : json.getAsJsonArray("tags")) {
                if (tag.isJsonPrimitive()) {
                    emote.tags().add(tag.getAsString());
                }
            }
        }

        if (!json.has("tracks") || !json.get("tracks").isJsonObject()) {
            throw new EmoteParseException("Emote '" + id + "' has no tracks object");
        }

        JsonObject tracks = json.getAsJsonObject("tracks");
        for (Map.Entry<String, JsonElement> entry : tracks.entrySet()) {
            EmotePart part = EmotePart.byKey(entry.getKey());
            if (part == null || !entry.getValue().isJsonArray()) {
                continue;
            }
            List<Keyframe> frames = new ArrayList<>();
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                Keyframe frame = readKeyframe(element);
                if (frame != null) {
                    frames.add(frame);
                }
            }
            if (!frames.isEmpty()) {
                emote.setTrack(part, new EmoteTrack(frames));
            }
        }

        if (emote.tracks().isEmpty()) {
            throw new EmoteParseException("Emote '" + id + "' has no usable keyframes");
        }
        return emote;
    }

    private static Keyframe readKeyframe(JsonElement element) {
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        float time = object.has("t") ? number(object, "t", 0.0F) : number(object, "time", 0.0F);

        float[] rot = triple(object, "rot", 0.0F);
        float[] pos = triple(object, "pos", 0.0F);
        float[] scale = triple(object, "scale", 1.0F);

        // Flat aliases, handy when hand-writing a file that only moves one axis.
        rot[0] = number(object, "rx", rot[0]);
        rot[1] = number(object, "ry", rot[1]);
        rot[2] = number(object, "rz", rot[2]);
        pos[0] = number(object, "px", pos[0]);
        pos[1] = number(object, "py", pos[1]);
        pos[2] = number(object, "pz", pos[2]);

        Easing easing = Easing.byName(string(object, "ease", string(object, "easing", "linear")));
        return new Keyframe(time, rot[0], rot[1], rot[2], pos[0], pos[1], pos[2], scale[0], scale[1], scale[2], easing);
    }

    public static JsonObject write(Emote emote) {
        JsonObject json = new JsonObject();
        json.addProperty("format", FORMAT_VERSION);
        json.addProperty("id", emote.id());
        json.addProperty("name", emote.name());
        if (!emote.author().isBlank()) {
            json.addProperty("author", emote.author());
        }
        if (!emote.description().isBlank()) {
            json.addProperty("description", emote.description());
        }
        json.addProperty("category", emote.category());
        if (!emote.tags().isEmpty()) {
            JsonArray tags = new JsonArray();
            emote.tags().forEach(tags::add);
            json.add("tags", tags);
        }
        json.addProperty("length", trim(emote.lengthTicks()));
        json.addProperty("loop", emote.loop());
        json.addProperty("blendIn", trim(emote.blendInTicks()));
        json.addProperty("blendOut", trim(emote.blendOutTicks()));
        if (emote.stopOnMove()) {
            json.addProperty("stopOnMove", true);
        }
        if (emote.speed() != 1.0F) {
            json.addProperty("speed", trim(emote.speed()));
        }

        JsonObject tracks = new JsonObject();
        for (EmotePart part : EmotePart.values()) {
            EmoteTrack track = emote.track(part);
            if (track == null || track.isEmpty()) {
                continue;
            }
            JsonArray frames = new JsonArray();
            for (Keyframe frame : track.keyframes()) {
                frames.add(writeKeyframe(frame));
            }
            tracks.add(part.key(), frames);
        }
        json.add("tracks", tracks);
        return json;
    }

    private static JsonObject writeKeyframe(Keyframe frame) {
        JsonObject object = new JsonObject();
        object.addProperty("t", trim(frame.time()));
        if (frame.rotX() != 0.0F || frame.rotY() != 0.0F || frame.rotZ() != 0.0F) {
            object.add("rot", array(frame.rotX(), frame.rotY(), frame.rotZ()));
        }
        if (frame.posX() != 0.0F || frame.posY() != 0.0F || frame.posZ() != 0.0F) {
            object.add("pos", array(frame.posX(), frame.posY(), frame.posZ()));
        }
        if (frame.scaleX() != 1.0F || frame.scaleY() != 1.0F || frame.scaleZ() != 1.0F) {
            object.add("scale", array(frame.scaleX(), frame.scaleY(), frame.scaleZ()));
        }
        if (frame.easing() != Easing.LINEAR) {
            object.addProperty("ease", frame.easing().serialisedName());
        }
        return object;
    }

    public static String toPrettyString(Emote emote) {
        return GSON.toJson(write(emote));
    }

    private static JsonArray array(float x, float y, float z) {
        JsonArray array = new JsonArray(3);
        array.add(trim(x));
        array.add(trim(y));
        array.add(trim(z));
        return array;
    }

    /** Rounds to 3 decimals so files stay readable and diff cleanly. */
    private static float trim(float value) {
        return Math.round(value * 1000.0F) / 1000.0F;
    }

    private static float[] triple(JsonObject object, String key, float fallback) {
        float[] result = {fallback, fallback, fallback};
        if (!object.has(key)) {
            return result;
        }
        JsonElement element = object.get(key);
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < 3 && i < array.size(); i++) {
                try {
                    result[i] = array.get(i).getAsFloat();
                } catch (RuntimeException ignored) {
                    // Leave the fallback in place for this axis.
                }
            }
        } else if (element.isJsonPrimitive()) {
            try {
                float single = element.getAsFloat();
                result[0] = single;
                result[1] = single;
                result[2] = single;
            } catch (RuntimeException ignored) {
                // Not a number, keep fallbacks.
            }
        }
        return result;
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (object.has(key) && object.get(key).isJsonPrimitive()) {
            return object.get(key).getAsString();
        }
        return fallback;
    }

    private static float number(JsonObject object, String key, float fallback) {
        if (object.has(key) && object.get(key).isJsonPrimitive()) {
            try {
                return object.get(key).getAsFloat();
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        if (object.has(key) && object.get(key).isJsonPrimitive()) {
            try {
                return object.get(key).getAsBoolean();
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
