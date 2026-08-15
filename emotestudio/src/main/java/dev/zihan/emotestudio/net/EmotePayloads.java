package dev.zihan.emotestudio.net;

import dev.zihan.emotestudio.EmoteStudioMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Wire format shared by the client and the (integrated or dedicated) server.
 *
 * <p>Custom emotes travel with their definition attached the first time they are used, so other
 * players can render an emote they have never seen. The payload is size-capped: an emote that does
 * not fit is played locally only rather than being allowed to flood the connection.
 */
public final class EmotePayloads {
    /** Upper bound on a serialised emote, in UTF-8 bytes. Comfortably above the largest built-in. */
    public static final int MAX_EMOTE_DATA = 48 * 1024;
    public static final int MAX_ID_LENGTH = 128;

    private EmotePayloads() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EmoteStudioMod.MOD_ID, path);
    }

    /**
     * Client asks the server to broadcast an emote it just started.
     *
     * @param emoteData serialised emote JSON, or an empty string when the emote is a built-in that
     *                  every client already has
     */
    public record PlayEmote(String emoteId, String emoteData, float speed) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlayEmote> TYPE = new CustomPacketPayload.Type<>(id("play_emote"));
        public static final StreamCodec<FriendlyByteBuf, PlayEmote> CODEC =
                CustomPacketPayload.codec(PlayEmote::write, PlayEmote::read);

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(emoteId, MAX_ID_LENGTH);
            buf.writeUtf(emoteData, MAX_EMOTE_DATA);
            buf.writeFloat(speed);
        }

        private static PlayEmote read(FriendlyByteBuf buf) {
            return new PlayEmote(buf.readUtf(MAX_ID_LENGTH), buf.readUtf(MAX_EMOTE_DATA), buf.readFloat());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client tells the server it stopped emoting. */
    public record StopEmote() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StopEmote> TYPE = new CustomPacketPayload.Type<>(id("stop_emote"));
        public static final StreamCodec<FriendlyByteBuf, StopEmote> CODEC =
                CustomPacketPayload.codec((payload, buf) -> {
                }, buf -> new StopEmote());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server tells clients what another player is doing.
     *
     * @param playing false means "this player stopped", in which case the other fields are ignored
     */
    public record EmoteState(UUID player, String emoteId, String emoteData, float speed, boolean playing)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<EmoteState> TYPE = new CustomPacketPayload.Type<>(id("emote_state"));
        public static final StreamCodec<FriendlyByteBuf, EmoteState> CODEC =
                CustomPacketPayload.codec(EmoteState::write, EmoteState::read);

        public static EmoteState stopped(UUID player) {
            return new EmoteState(player, "", "", 1.0F, false);
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUUID(player);
            buf.writeBoolean(playing);
            if (playing) {
                buf.writeUtf(emoteId, MAX_ID_LENGTH);
                buf.writeUtf(emoteData, MAX_EMOTE_DATA);
                buf.writeFloat(speed);
            }
        }

        private static EmoteState read(FriendlyByteBuf buf) {
            UUID player = buf.readUUID();
            boolean playing = buf.readBoolean();
            if (!playing) {
                return EmoteState.stopped(player);
            }
            return new EmoteState(player, buf.readUtf(MAX_ID_LENGTH), buf.readUtf(MAX_EMOTE_DATA), buf.readFloat(), true);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Registers every payload type on both directions. Must run on client and server alike. */
    public static void registerTypes() {
        PayloadTypeRegistry.serverboundPlay().register(PlayEmote.TYPE, PlayEmote.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StopEmote.TYPE, StopEmote.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EmoteState.TYPE, EmoteState.CODEC);
    }
}
