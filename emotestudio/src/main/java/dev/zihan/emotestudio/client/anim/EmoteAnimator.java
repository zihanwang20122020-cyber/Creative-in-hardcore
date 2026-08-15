package dev.zihan.emotestudio.client.anim;

import dev.zihan.emotestudio.client.EmoteStudioClient;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.EmotePose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side registry of who is emoting right now.
 *
 * <p>Playback is driven by a monotonic tick counter rather than the world clock so that emotes keep
 * their timing when the game is paused in singleplayer or the world time is changed.
 *
 * <p>Nothing here touches the player's position, velocity or rotation. Emotes are drawn, not
 * simulated, which is why they are safe to run on your own world or a LAN world without the server
 * ever seeing anomalous movement.
 */
public final class EmoteAnimator {
    private static final EmoteAnimator INSTANCE = new EmoteAnimator();

    private final Map<UUID, Playback> playbacks = new ConcurrentHashMap<>();

    /** Per-thread so parallel render-state extraction can never trample a pose mid-sample. */
    private final ThreadLocal<EmotePose> scratch = ThreadLocal.withInitial(EmotePose::new);

    private float ticks;

    /**
     * Editor preview. While set, the local player renders this emote at this exact time instead of
     * whatever they are otherwise playing, so scrubbing the timeline moves the character live.
     */
    private Emote previewEmote;
    private float previewTime;

    private EmoteAnimator() {
    }

    public static EmoteAnimator get() {
        return INSTANCE;
    }

    /** Current time base including the frame's partial tick. */
    public float now(float partialTick) {
        return ticks + partialTick;
    }

    public float now() {
        return ticks;
    }

    /**
     * Partial tick for the frame being drawn. The render hooks sit inside {@code setupAnim} and
     * {@code setupRotations}, neither of which is handed one, so it is read back from the client.
     */
    public float currentPartialTick() {
        Minecraft minecraft = Minecraft.getInstance();
        // Ignore the pause freeze: emotes should keep playing while the editor screen is open.
        return minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public void tick(Minecraft minecraft) {
        ticks += 1.0F;

        LocalPlayer localPlayer = minecraft.player;
        Iterator<Map.Entry<UUID, Playback>> iterator = playbacks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Playback> entry = iterator.next();
            Playback playback = entry.getValue();
            if (playback.finished(ticks)) {
                iterator.remove();
                continue;
            }
            if (localPlayer != null && entry.getKey().equals(localPlayer.getUUID())) {
                if (playback.emote().stopOnMove() && isMoving(localPlayer) && !playback.stopping()) {
                    EmoteStudioClient.stopOwnEmote();
                }
            }
        }
    }

    private static boolean isMoving(LocalPlayer player) {
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        return (dx * dx + dz * dz) > 1.0E-4 || player.input.keyPresses.jump();
    }

    public void start(UUID player, Emote emote, float speed) {
        if (player == null || emote == null) {
            return;
        }
        playbacks.put(player, new Playback(emote, ticks, speed));
    }

    /** Asks an emote to fade out. It keeps rendering until the fade completes. */
    public void stop(UUID player) {
        Playback playback = playbacks.get(player);
        if (playback != null) {
            playback.requestStop(ticks);
        }
    }

    /** Drops an emote without a fade, used when a player leaves or the world unloads. */
    public void clear(UUID player) {
        playbacks.remove(player);
    }

    public void clearAll() {
        playbacks.clear();
    }

    public Playback playbackOf(UUID player) {
        return player == null ? null : playbacks.get(player);
    }

    public boolean isEmoting(UUID player) {
        Playback playback = playbackOf(player);
        return playback != null && !playback.stopping();
    }

    public Emote currentEmote(UUID player) {
        Playback playback = playbackOf(player);
        return playback == null ? null : playback.emote();
    }

    public boolean isEmoting(Player player) {
        return player != null && isEmoting(player.getUUID());
    }

    /** Points the editor preview at {@code emote}, frozen at {@code time} ticks. */
    public void setPreview(Emote emote, float time) {
        this.previewEmote = emote;
        this.previewTime = time;
    }

    public void clearPreview() {
        this.previewEmote = null;
    }

    public boolean hasPreview() {
        return previewEmote != null;
    }

    /**
     * Samples a player's emote into a caller-provided pose.
     *
     * @return the blend weight, or 0 when the player is not emoting (in which case {@code out} is
     *         left untouched)
     */
    public float samplePose(UUID player, float partialTick, EmotePose out) {
        if (previewEmote != null && isLocalPlayer(player)) {
            previewEmote.sample(previewTime, out);
            return 1.0F;
        }
        Playback playback = playbacks.get(player);
        if (playback == null) {
            return 0.0F;
        }
        float time = now(partialTick);
        float weight = playback.weight(time);
        if (weight <= 0.0F) {
            return 0.0F;
        }
        playback.sample(time, out);
        return weight;
    }

    /**
     * Render-path entry point: resolves a render state's entity id to a player and samples it.
     *
     * @return the blend weight, or 0 when that entity is not a player or is not emoting
     */
    public float sampleForEntityId(int entityId, EmotePose out) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        if (!(minecraft.level.getEntity(entityId) instanceof Player player)) {
            return 0.0F;
        }
        return samplePose(player.getUUID(), currentPartialTick(), out);
    }

    private static boolean isLocalPlayer(UUID uuid) {
        LocalPlayer local = Minecraft.getInstance().player;
        return local != null && local.getUUID().equals(uuid);
    }

    /** Scratch pose for render-path callers that only need it for the duration of a call. */
    public EmotePose scratchPose() {
        return scratch.get();
    }
}
