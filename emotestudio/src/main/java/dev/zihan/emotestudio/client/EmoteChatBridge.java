package dev.zihan.emotestudio.client;

import com.mojang.authlib.GameProfile;
import dev.zihan.emotestudio.EmoteStudioMod;
import dev.zihan.emotestudio.client.anim.EmoteAnimator;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.io.EmoteRepository;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Emote synchronisation on servers that do not have the mod.
 *
 * <p>A vanilla server drops custom payload packets on the floor — {@code handleCustomPayload} is
 * literally an empty method — so there is no way for one client to hand another client arbitrary
 * data through it. Chat is the only channel a vanilla server relays between players that the sender
 * controls the contents of, so that is what this uses.
 *
 * <p>The message is deliberately readable: a player without the mod sees {@code [emote] wave}
 * rather than a wall of encoded noise, and players with the mod see the animation while the line is
 * hidden from their chat.
 *
 * <p>Because this posts real chat messages under the player's name, it is rate limited, it is
 * announced the first time it happens, and it can be turned off with {@code /emote chatsync off}.
 */
public final class EmoteChatBridge {
    /** Kept short and ASCII so server chat filters and formatting plugins leave it intact. */
    private static final String MARKER = "[emote]";
    private static final String STOP_TOKEN = "stop";
    private static final Pattern PATTERN =
            Pattern.compile("\\[emote]\\s+([a-z0-9_.\\-]{1,64})", Pattern.CASE_INSENSITIVE);

    /**
     * Minimum gap between two chat sends. Vanilla's spam filter charges 20 points per message and
     * refunds one per tick, kicking at 200, so one message every 30 ticks stays far below the line.
     */
    private static final long MIN_SEND_INTERVAL_MS = 1_500L;

    /** Chat-driven loops expire on their own, in case the sender's stop message never arrives. */
    private static final long CHAT_EMOTE_TIMEOUT_TICKS = 1200L;

    private static final Map<UUID, Long> CHAT_STARTED = new HashMap<>();

    private static long lastSendMs;
    private static boolean warnedAboutChat;

    private EmoteChatBridge() {
    }

    public static void register() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, timestamp) ->
                !handleIncoming(message.getString(), sender == null ? null : sender.id()));

        // Servers with chat plugins usually re-emit player chat as a system message, which arrives
        // without a sender, so the name has to be recovered from the text itself.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || !handleIncoming(message.getString(), null));
    }

    /** Drops chat-started emotes that have been running too long. Called from the client tick. */
    public static void tick(long tick) {
        if (CHAT_STARTED.isEmpty()) {
            return;
        }
        CHAT_STARTED.entrySet().removeIf(entry -> {
            if (tick - entry.getValue() < CHAT_EMOTE_TIMEOUT_TICKS) {
                return false;
            }
            EmoteAnimator.get().stop(entry.getKey());
            return true;
        });
    }

    /**
     * Announces an emote through chat.
     *
     * @return true when a message was actually sent; false when it was suppressed, in which case the
     *         emote still plays locally
     */
    public static boolean sendEmote(Emote emote) {
        if (!EmoteStudioClient.config().chatSync()) {
            return false;
        }
        if (!send(emote.id())) {
            return false;
        }
        warnOnce();
        return true;
    }

    public static void sendStop() {
        if (EmoteStudioClient.config().chatSync()) {
            send(STOP_TOKEN);
        }
    }

    private static boolean send(String token) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastSendMs < MIN_SEND_INTERVAL_MS) {
            // Too soon: the emote still plays here, it just is not announced.
            return false;
        }
        lastSendMs = now;
        minecraft.getConnection().sendChat(MARKER + " " + token);
        return true;
    }

    private static void warnOnce() {
        if (warnedAboutChat) {
            return;
        }
        warnedAboutChat = true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component
                    .translatable("message.emotestudio.chat_sync_notice")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Reacts to an incoming chat line.
     *
     * @return true when the line was one of ours and should be hidden from chat
     */
    private static boolean handleIncoming(String text, UUID senderId) {
        String token = extractToken(text);
        if (token == null) {
            return false;
        }

        UUID player = senderId != null ? senderId : resolveSenderFromText(text);
        if (player == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && player.equals(minecraft.player.getUUID())) {
            // Our own message coming back: we already played it locally.
            return true;
        }

        if (token.equals(STOP_TOKEN)) {
            EmoteAnimator.get().stop(player);
            CHAT_STARTED.remove(player);
            return true;
        }

        Emote emote = EmoteRepository.get().byId(token);
        if (emote == null) {
            // Unknown emote: leave the line visible so the player at least reads what happened.
            EmoteStudioMod.LOGGER.debug("Chat sync mentioned unknown emote '{}'.", token);
            return false;
        }
        EmoteAnimator.get().start(player, emote, 1.0F);
        CHAT_STARTED.put(player, (long) EmoteAnimator.get().now());
        return true;
    }

    /**
     * Pulls the emote token out of a chat line, or returns null when the line is not one of ours.
     *
     * <p>Servers rarely deliver chat untouched: ranks, colours, arrows and nicknames all get spliced
     * in around the message. The marker is therefore searched for anywhere in the line rather than
     * anchored to the start.
     */
    public static String extractToken(String text) {
        if (text == null || !text.toLowerCase(Locale.ROOT).contains(MARKER)) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Finds which player sent a system-formatted chat line by looking for a tab-list name in it.
     * Longest name first, so "Bob" inside "BobBuilder" cannot win over the real sender.
     */
    private static UUID resolveSenderFromText(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return null;
        }
        UUID best = null;
        int bestLength = 0;
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            GameProfile profile = info.getProfile();
            String name = profile.name();
            if (name == null || name.length() <= bestLength) {
                continue;
            }
            if (text.contains(name)) {
                best = profile.id();
                bestLength = name.length();
            }
        }
        return best;
    }
}
