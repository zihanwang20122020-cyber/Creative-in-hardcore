package dev.zihan.emotestudio;

import dev.zihan.emotestudio.net.EmotePayloads;
import dev.zihan.emotestudio.server.EmoteServerRelay;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. Everything here runs on both the client and the (integrated or dedicated)
 * server; the animation, the editor and the whole GUI live on the client side only.
 */
public final class EmoteStudioMod implements ModInitializer {
    public static final String MOD_ID = "emotestudio";
    public static final String MOD_NAME = "Emote Studio";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitialize() {
        EmotePayloads.registerTypes();
        EmoteServerRelay.register();
        LOGGER.info("{} initialised.", MOD_NAME);
    }
}
