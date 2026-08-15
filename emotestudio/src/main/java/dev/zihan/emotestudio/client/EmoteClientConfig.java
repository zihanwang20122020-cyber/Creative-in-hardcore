package dev.zihan.emotestudio.client;

import dev.zihan.emotestudio.EmoteStudioMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Client-side preferences: the eight quick-slot emotes, favourites, and a few playback options. */
public final class EmoteClientConfig {
    public static final int QUICK_SLOTS = 8;

    private static final String FILE_NAME = EmoteStudioMod.MOD_ID + ".properties";

    private final String[] quickSlots = new String[QUICK_SLOTS];
    private final Set<String> favourites = new LinkedHashSet<>();

    /** Switch to third person while emoting, so you can actually see what you are doing. */
    private boolean autoThirdPerson = true;
    /** Cancel the current emote as soon as you move, regardless of the emote's own setting. */
    private boolean stopOnMove;
    /** Show the emote name above other players while they emote. */
    private boolean showEmoteLabels = true;
    /**
     * On a server without the mod, announce emotes through chat so other players running the mod
     * can still see them. Sends a real chat message under your name, so it can be turned off.
     */
    private boolean chatSync = true;
    /**
     * On a server without the mod, also perform the emote physically — body and head rotation, arm
     * swings, crouching and hops — so players without the mod see something too. This one really
     * moves the player, within what ordinary input allows.
     */
    private boolean physicalPerformance = true;

    private EmoteClientConfig() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static EmoteClientConfig load() {
        EmoteClientConfig config = new EmoteClientConfig();
        Path path = file();
        if (!Files.exists(path)) {
            config.save();
            return config;
        }
        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(path)) {
            properties.load(stream);
        } catch (IOException e) {
            EmoteStudioMod.LOGGER.warn("Could not read {}, using defaults: {}", FILE_NAME, e.toString());
            return config;
        }

        for (int i = 0; i < QUICK_SLOTS; i++) {
            config.quickSlots[i] = properties.getProperty("quickSlot" + (i + 1), "").trim();
        }
        String favourites = properties.getProperty("favourites", "");
        for (String entry : favourites.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                config.favourites.add(trimmed);
            }
        }
        config.autoThirdPerson = Boolean.parseBoolean(properties.getProperty("autoThirdPerson", "true"));
        config.stopOnMove = Boolean.parseBoolean(properties.getProperty("stopOnMove", "false"));
        config.showEmoteLabels = Boolean.parseBoolean(properties.getProperty("showEmoteLabels", "true"));
        config.chatSync = Boolean.parseBoolean(properties.getProperty("chatSync", "true"));
        config.physicalPerformance = Boolean.parseBoolean(
                properties.getProperty("physicalPerformance", "true"));
        return config;
    }

    public void save() {
        Properties properties = new Properties();
        for (int i = 0; i < QUICK_SLOTS; i++) {
            properties.setProperty("quickSlot" + (i + 1), quickSlots[i] == null ? "" : quickSlots[i]);
        }
        properties.setProperty("favourites", String.join(",", favourites));
        properties.setProperty("autoThirdPerson", Boolean.toString(autoThirdPerson));
        properties.setProperty("stopOnMove", Boolean.toString(stopOnMove));
        properties.setProperty("showEmoteLabels", Boolean.toString(showEmoteLabels));
        properties.setProperty("chatSync", Boolean.toString(chatSync));
        properties.setProperty("physicalPerformance", Boolean.toString(physicalPerformance));

        try {
            Files.createDirectories(file().getParent());
            try (OutputStream stream = Files.newOutputStream(file())) {
                properties.store(stream, "Emote Studio client settings");
            }
        } catch (IOException e) {
            EmoteStudioMod.LOGGER.error("Could not write {}: {}", FILE_NAME, e.toString());
        }
    }

    public String quickSlot(int index) {
        return index >= 0 && index < QUICK_SLOTS ? quickSlots[index] : null;
    }

    public void setQuickSlot(int index, String emoteId) {
        if (index >= 0 && index < QUICK_SLOTS) {
            quickSlots[index] = emoteId == null ? "" : emoteId;
            save();
        }
    }

    public List<String> favourites() {
        return new ArrayList<>(favourites);
    }

    public boolean isFavourite(String emoteId) {
        return favourites.contains(emoteId);
    }

    public void toggleFavourite(String emoteId) {
        if (!favourites.remove(emoteId)) {
            favourites.add(emoteId);
        }
        save();
    }

    public boolean autoThirdPerson() {
        return autoThirdPerson;
    }

    public void setAutoThirdPerson(boolean autoThirdPerson) {
        this.autoThirdPerson = autoThirdPerson;
        save();
    }

    public boolean stopOnMove() {
        return stopOnMove;
    }

    public void setStopOnMove(boolean stopOnMove) {
        this.stopOnMove = stopOnMove;
        save();
    }

    public boolean showEmoteLabels() {
        return showEmoteLabels;
    }

    public boolean chatSync() {
        return chatSync;
    }

    public void setChatSync(boolean chatSync) {
        this.chatSync = chatSync;
        save();
    }

    public boolean physicalPerformance() {
        return physicalPerformance;
    }

    public void setPhysicalPerformance(boolean physicalPerformance) {
        this.physicalPerformance = physicalPerformance;
        save();
    }

    public void setShowEmoteLabels(boolean showEmoteLabels) {
        this.showEmoteLabels = showEmoteLabels;
        save();
    }
}
