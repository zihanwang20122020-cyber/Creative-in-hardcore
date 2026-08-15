package dev.zihan.emotestudio.server;

import dev.zihan.emotestudio.EmoteStudioMod;
import dev.zihan.emotestudio.net.EmotePayloads;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Relays emotes between players. Purely cosmetic: the server never moves anyone, it only forwards
 * "this player is doing this animation" to the other clients.
 *
 * <p>Works the same in singleplayer (where the integrated server is the only listener), on a LAN
 * world, and on a dedicated server. Vanilla clients simply never send or receive these packets.
 */
public final class EmoteServerRelay {
    /** What each player is currently doing, so someone logging in mid-emote sees it too. */
    private static final Map<UUID, Active> ACTIVE = new ConcurrentHashMap<>();

    private record Active(String emoteId, String emoteData, float speed) {
    }

    private EmoteServerRelay() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(EmotePayloads.PlayEmote.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            String emoteId = payload.emoteId();
            if (emoteId.isBlank()) {
                return;
            }
            Active active = new Active(emoteId, payload.emoteData(), sanitiseSpeed(payload.speed()));
            ACTIVE.put(sender.getUUID(), active);
            broadcast(sender, new EmotePayloads.EmoteState(
                    sender.getUUID(), active.emoteId(), active.emoteData(), active.speed(), true));
        });

        ServerPlayNetworking.registerGlobalReceiver(EmotePayloads.StopEmote.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            ACTIVE.remove(sender.getUUID());
            broadcast(sender, EmotePayloads.EmoteState.stopped(sender.getUUID()));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer joining = handler.getPlayer();
            // Catch the newcomer up on everyone already emoting.
            for (Map.Entry<UUID, Active> entry : ACTIVE.entrySet()) {
                if (entry.getKey().equals(joining.getUUID())) {
                    continue;
                }
                Active active = entry.getValue();
                ServerPlayNetworking.send(joining, new EmotePayloads.EmoteState(
                        entry.getKey(), active.emoteId(), active.emoteData(), active.speed(), true));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUUID();
            if (ACTIVE.remove(uuid) != null) {
                broadcastToAll(server, EmotePayloads.EmoteState.stopped(uuid));
            }
        });

        EmoteStudioMod.LOGGER.info("Emote relay ready.");
    }

    private static float sanitiseSpeed(float speed) {
        if (!Float.isFinite(speed) || speed <= 0.0F) {
            return 1.0F;
        }
        return Math.min(speed, 8.0F);
    }

    private static void broadcast(ServerPlayer source, EmotePayloads.EmoteState state) {
        // The source client is already playing the emote locally, so it is skipped here.
        for (ServerPlayer target : PlayerLookup.level(source.level())) {
            if (target != source && ServerPlayNetworking.canSend(target, EmotePayloads.EmoteState.TYPE)) {
                ServerPlayNetworking.send(target, state);
            }
        }
    }

    private static void broadcastToAll(MinecraftServer server, EmotePayloads.EmoteState state) {
        for (ServerPlayer target : PlayerLookup.all(server)) {
            if (ServerPlayNetworking.canSend(target, EmotePayloads.EmoteState.TYPE)) {
                ServerPlayNetworking.send(target, state);
            }
        }
    }
}
