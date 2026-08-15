package dev.zihan.emotestudio.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zihan.emotestudio.EmoteStudioMod;
import dev.zihan.emotestudio.client.anim.EmoteAnimator;
import dev.zihan.emotestudio.client.gui.EmoteBrowserScreen;
import dev.zihan.emotestudio.client.gui.EmoteWheelScreen;
import dev.zihan.emotestudio.client.gui.editor.EmoteEditorScreen;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.io.EmoteJson;
import dev.zihan.emotestudio.emote.io.EmoteRepository;
import dev.zihan.emotestudio.net.EmotePayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.StringReader;

/**
 * Client entrypoint: key bindings, the emote playback loop, and the bridge between the local
 * player's emotes and the rest of the server.
 */
public final class EmoteStudioClient implements ClientModInitializer {

    public static KeyMapping openWheelKey;
    public static KeyMapping openBrowserKey;
    public static KeyMapping openEditorKey;
    public static KeyMapping stopEmoteKey;
    private static final KeyMapping[] QUICK_SLOT_KEYS = new KeyMapping[8];

    private static EmoteClientConfig config;

    @Override
    public void onInitializeClient() {
        config = EmoteClientConfig.load();
        EmoteRepository.get().reload();

        registerKeys();
        registerReceivers();
        EmoteChatBridge.register();
        registerTicking();
        EmoteClientCommands.register();

        EmoteStudioMod.LOGGER.info("Emote Studio client ready with {} emotes.", EmoteRepository.get().all().size());
    }

    public static EmoteClientConfig config() {
        return config;
    }

    private void registerKeys() {
        KeyMapping.Category category = KeyMapping.Category.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(EmoteStudioMod.MOD_ID, "emotes"));

        openWheelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + EmoteStudioMod.MOD_ID + ".wheel", InputConstants.Type.KEYSYM, InputConstants.KEY_B, category));
        openBrowserKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + EmoteStudioMod.MOD_ID + ".browser", InputConstants.Type.KEYSYM, InputConstants.KEY_J, category));
        openEditorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + EmoteStudioMod.MOD_ID + ".editor", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));
        stopEmoteKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + EmoteStudioMod.MOD_ID + ".stop", InputConstants.Type.KEYSYM, InputConstants.KEY_X, category));

        for (int i = 0; i < QUICK_SLOT_KEYS.length; i++) {
            QUICK_SLOT_KEYS[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key." + EmoteStudioMod.MOD_ID + ".slot" + (i + 1),
                    InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));
        }
    }

    private void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(EmotePayloads.EmoteState.TYPE, (payload, context) -> {
            if (!payload.playing()) {
                EmoteAnimator.get().stop(payload.player());
                return;
            }
            Emote emote = resolveIncoming(payload.emoteId(), payload.emoteData());
            if (emote == null) {
                EmoteStudioMod.LOGGER.debug("Ignoring unknown emote '{}' from the server.", payload.emoteId());
                return;
            }
            EmoteAnimator.get().start(payload.player(), emote, payload.speed());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EmoteAnimator.get().clearAll());
    }

    /**
     * Turns an incoming emote into something renderable: a local copy when we already have that id,
     * otherwise the definition the sender attached.
     */
    private static Emote resolveIncoming(String emoteId, String emoteData) {
        Emote known = EmoteRepository.get().byId(emoteId);
        if (known != null) {
            return known;
        }
        if (emoteData == null || emoteData.isBlank()) {
            return null;
        }
        try {
            Emote received = EmoteJson.read(new StringReader(emoteData), emoteId);
            EmoteRepository.get().putRemote(received);
            return received;
        } catch (RuntimeException e) {
            EmoteStudioMod.LOGGER.warn("Rejected a malformed emote '{}' from the server: {}", emoteId, e.toString());
            return null;
        }
    }

    private void registerTicking() {
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            EmoteAnimator.get().tick(minecraft);
            EmoteChatBridge.tick((long) EmoteAnimator.get().now());

            if (minecraft.player == null) {
                return;
            }

            while (openWheelKey.consumeClick()) {
                minecraft.setScreenAndShow(new EmoteWheelScreen());
            }
            while (openBrowserKey.consumeClick()) {
                minecraft.setScreenAndShow(new EmoteBrowserScreen(null));
            }
            while (openEditorKey.consumeClick()) {
                minecraft.setScreenAndShow(EmoteEditorScreen.forNewEmote());
            }
            while (stopEmoteKey.consumeClick()) {
                stopOwnEmote();
            }
            for (int slot = 0; slot < QUICK_SLOT_KEYS.length; slot++) {
                while (QUICK_SLOT_KEYS[slot].consumeClick()) {
                    String id = config.quickSlot(slot);
                    if (id != null && !id.isBlank()) {
                        playOwnEmote(id);
                    }
                }
            }
        });
    }

    /**
     * Plays an emote on the local player and, when the server speaks our protocol, asks it to show
     * the same thing to everyone else.
     *
     * @return true when the emote existed and started
     */
    public static boolean playOwnEmote(String emoteId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        Emote emote = EmoteRepository.get().byId(emoteId);
        if (emote == null) {
            return false;
        }
        return playOwnEmote(emote);
    }

    public static boolean playOwnEmote(Emote emote) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || emote == null) {
            return false;
        }

        EmoteAnimator.get().start(minecraft.player.getUUID(), emote, 1.0F);

        if (!ClientPlayNetworking.canSend(EmotePayloads.PlayEmote.TYPE)) {
            // The server does not have the mod, so it will never relay a custom payload. Fall back
            // to chat, which every server relays, so other players running the mod still see this.
            EmoteChatBridge.sendEmote(emote);
            return true;
        }

        String payloadData = "";
        if (emote.source() != Emote.Source.BUILTIN) {
            String serialised = EmoteJson.toPrettyString(emote);
            if (serialised.length() <= EmotePayloads.MAX_EMOTE_DATA) {
                payloadData = serialised;
            } else {
                minecraft.player.sendOverlayMessage(Component
                        .translatable("message.emotestudio.too_big", emote.name())
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        ClientPlayNetworking.send(new EmotePayloads.PlayEmote(emote.id(), payloadData, 1.0F));
        return true;
    }

    public static void stopOwnEmote() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        EmoteAnimator.get().stop(minecraft.player.getUUID());
        if (ClientPlayNetworking.canSend(EmotePayloads.StopEmote.TYPE)) {
            ClientPlayNetworking.send(new EmotePayloads.StopEmote());
        } else {
            EmoteChatBridge.sendStop();
        }
    }
}
