package dev.zihan.emotestudio.emote.io;

import com.google.gson.JsonObject;
import dev.zihan.emotestudio.EmoteStudioMod;
import dev.zihan.emotestudio.emote.Emote;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Owns every emote the game knows about: the built-ins shipped in the jar, the player's own files
 * under {@code <game dir>/emotes}, and anything received from other players this session.
 *
 * <p>User files win over built-ins with the same id, so a player can override {@code moonwalk}
 * without touching the jar.
 */
public final class EmoteRepository {
    public static final String USER_FOLDER = "emotes";
    public static final String FILE_SUFFIX = ".emote.json";
    private static final String BUILTIN_ROOT = "assets/" + EmoteStudioMod.MOD_ID + "/emotes";
    private static final String BUILTIN_INDEX = "/" + BUILTIN_ROOT + "/index.json";

    private static final EmoteRepository INSTANCE = new EmoteRepository();

    private final Map<String, Emote> emotes = new ConcurrentHashMap<>();
    private final List<Emote> sorted = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private volatile boolean loaded;

    private EmoteRepository() {
    }

    public static EmoteRepository get() {
        return INSTANCE;
    }

    public Path userFolder() {
        return FabricLoader.getInstance().getGameDir().resolve(USER_FOLDER);
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** Loads built-ins and user emotes, replacing everything except remote emotes. */
    public synchronized int reload() {
        emotes.values().removeIf(emote -> emote.source() != Emote.Source.REMOTE);

        int builtin = loadBuiltins();
        int user = loadUserEmotes();
        reindex();
        loaded = true;
        EmoteStudioMod.LOGGER.info("Loaded {} built-in and {} user emotes ({} total).", builtin, user, emotes.size());
        return emotes.size();
    }

    private int loadBuiltins() {
        List<String> ids = builtinIds();
        int count = 0;
        for (String id : ids) {
            String resource = "/" + BUILTIN_ROOT + "/" + id + FILE_SUFFIX;
            try (InputStream stream = EmoteRepository.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    EmoteStudioMod.LOGGER.warn("Built-in emote '{}' is listed but missing from the jar.", id);
                    continue;
                }
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    Emote emote = EmoteJson.read(reader, id);
                    emote.setSource(Emote.Source.BUILTIN);
                    emotes.put(emote.id(), emote);
                    count++;
                }
            } catch (IOException | RuntimeException e) {
                EmoteStudioMod.LOGGER.error("Failed to read built-in emote '{}': {}", id, e.toString());
            }
        }
        return count;
    }

    /**
     * Lists built-in ids. Walking the jar's resource directory is preferred; the generated
     * {@code index.json} is the fallback for environments where that walk is not available.
     */
    private List<String> builtinIds() {
        Set<String> ids = new LinkedHashSet<>();

        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(EmoteStudioMod.MOD_ID);
        if (container.isPresent()) {
            Optional<Path> root = container.get().findPath(BUILTIN_ROOT);
            if (root.isPresent() && Files.isDirectory(root.get())) {
                try (Stream<Path> files = Files.list(root.get())) {
                    files.map(path -> path.getFileName().toString())
                            .filter(fileName -> fileName.endsWith(FILE_SUFFIX))
                            .map(fileName -> fileName.substring(0, fileName.length() - FILE_SUFFIX.length()))
                            .sorted()
                            .forEach(ids::add);
                } catch (IOException e) {
                    EmoteStudioMod.LOGGER.warn("Could not list built-in emotes, falling back to index: {}", e.toString());
                }
            }
        }

        if (ids.isEmpty()) {
            try (InputStream stream = EmoteRepository.class.getResourceAsStream(BUILTIN_INDEX)) {
                if (stream != null) {
                    try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        JsonObject index = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                        if (index.has("emotes")) {
                            index.getAsJsonArray("emotes").forEach(element -> ids.add(element.getAsString()));
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                EmoteStudioMod.LOGGER.error("Could not read the built-in emote index: {}", e.toString());
            }
        }
        return new ArrayList<>(ids);
    }

    private int loadUserEmotes() {
        Path folder = userFolder();
        try {
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
                writeReadme(folder);
                return 0;
            }
        } catch (IOException e) {
            EmoteStudioMod.LOGGER.error("Could not create the emotes folder at {}: {}", folder, e.toString());
            return 0;
        }

        int count = 0;
        try (Stream<Path> files = Files.walk(folder, 4)) {
            List<Path> candidates = files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return fileName.endsWith(FILE_SUFFIX) || fileName.endsWith(".json");
                    })
                    .sorted()
                    .toList();
            for (Path path : candidates) {
                if (path.getFileName().toString().equalsIgnoreCase("index.json")) {
                    continue;
                }
                if (loadUserEmote(path)) {
                    count++;
                }
            }
        } catch (IOException e) {
            EmoteStudioMod.LOGGER.error("Could not scan the emotes folder: {}", e.toString());
        }
        return count;
    }

    private boolean loadUserEmote(Path path) {
        String fileName = path.getFileName().toString();
        String fallbackId = fileName.endsWith(FILE_SUFFIX)
                ? fileName.substring(0, fileName.length() - FILE_SUFFIX.length())
                : fileName.substring(0, fileName.lastIndexOf('.'));
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Emote emote = EmoteJson.read(reader, fallbackId);
            emote.setSource(Emote.Source.USER);
            emote.setFile(path);
            emotes.put(emote.id(), emote);
            return true;
        } catch (IOException | RuntimeException e) {
            EmoteStudioMod.LOGGER.error("Failed to read emote file {}: {}", path.getFileName(), e.toString());
            return false;
        }
    }

    private void writeReadme(Path folder) {
        String readme = """
                Emote Studio — custom emotes
                ============================

                Drop `.emote.json` files in this folder and they load on the next
                `/emote reload`, or when you press the reload button in the emote browser.

                The in-game editor (default key: K, or `/emote editor`) writes its emotes here,
                so you normally never have to touch this folder by hand.

                File format
                -----------
                  length      total duration, in ticks (20 ticks = 1 second)
                  loop        true to repeat forever, false to play once
                  blendIn     ticks spent fading from the normal pose into the emote
                  blendOut    ticks spent fading back out (non-looping emotes only)
                  stopOnMove  true to cancel the emote as soon as the player moves
                  tracks      one entry per animated part: root, head, body,
                              rightArm, leftArm, rightLeg, leftLeg

                Every keyframe takes a time `t` in ticks plus any of:
                  rot    [x, y, z]  rotation in degrees
                  pos    [x, y, z]  offset in model pixels (1/16 block)
                  scale  [x, y, z]  scale multiplier
                  ease              curve towards the next keyframe, e.g. sine_in_out

                `root` is not a body part: it moves and turns the whole player, which is how
                emotes such as float or moonwalk get their travel.
                """;
        try {
            Files.writeString(folder.resolve("README.txt"), readme, StandardCharsets.UTF_8);
        } catch (IOException e) {
            EmoteStudioMod.LOGGER.warn("Could not write the emotes README: {}", e.toString());
        }
    }

    private void reindex() {
        sorted.clear();
        sorted.addAll(emotes.values());
        sorted.sort(Comparator.comparing(Emote::category).thenComparing(Emote::name, String.CASE_INSENSITIVE_ORDER));

        Set<String> found = new LinkedHashSet<>();
        sorted.forEach(emote -> found.add(emote.category()));
        List<String> ordered = new ArrayList<>(found);
        ordered.sort(String.CASE_INSENSITIVE_ORDER);
        categories.clear();
        categories.addAll(ordered);
    }

    public Emote byId(String id) {
        return id == null ? null : emotes.get(Emote.sanitiseId(id));
    }

    public boolean contains(String id) {
        return byId(id) != null;
    }

    public List<Emote> all() {
        return List.copyOf(sorted);
    }

    public List<String> categories() {
        return List.copyOf(categories);
    }

    public List<Emote> search(String query, String category) {
        List<Emote> result = new ArrayList<>();
        for (Emote emote : sorted) {
            if (category != null && !category.isBlank() && !emote.category().equalsIgnoreCase(category)) {
                continue;
            }
            if (emote.matches(query)) {
                result.add(emote);
            }
        }
        return result;
    }

    public List<String> ids() {
        List<String> result = new ArrayList<>(emotes.keySet());
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    /** Registers an emote received from another player, without touching disk. */
    public void putRemote(Emote emote) {
        if (emote == null) {
            return;
        }
        Emote existing = emotes.get(emote.id());
        if (existing != null && existing.source() != Emote.Source.REMOTE) {
            // Never let the network shadow a local emote of the same name.
            return;
        }
        emote.setSource(Emote.Source.REMOTE);
        emotes.put(emote.id(), emote);
        reindex();
    }

    /**
     * Writes an emote into the user folder and registers it. Returns the file it was written to.
     */
    public synchronized Path save(Emote emote) throws IOException {
        Path folder = userFolder();
        Files.createDirectories(folder);
        Path target = emote.file();
        if (target == null || !target.startsWith(folder)) {
            target = folder.resolve(emote.id() + FILE_SUFFIX);
        }
        Files.writeString(target, EmoteJson.toPrettyString(emote), StandardCharsets.UTF_8);
        emote.setSource(Emote.Source.USER);
        emote.setFile(target);
        emotes.put(emote.id(), emote);
        reindex();
        return target;
    }

    /** Deletes a user emote from disk and from the index. Built-ins are left alone. */
    public synchronized boolean delete(Emote emote) throws IOException {
        if (emote == null || emote.source() != Emote.Source.USER) {
            return false;
        }
        if (emote.file() != null) {
            Files.deleteIfExists(emote.file());
        }
        emotes.remove(emote.id());
        reindex();
        return true;
    }

    /** A free id derived from {@code base}, e.g. {@code wave_2} when {@code wave} is taken. */
    public String freeId(String base) {
        String root = Emote.sanitiseId(base);
        if (!contains(root)) {
            return root;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = root + "_" + i;
            if (!contains(candidate)) {
                return candidate;
            }
        }
        return root + "_copy";
    }
}
