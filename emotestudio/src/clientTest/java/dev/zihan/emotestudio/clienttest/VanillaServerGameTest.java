package dev.zihan.emotestudio.clienttest;

import dev.zihan.emotestudio.client.EmoteStudioClient;
import dev.zihan.emotestudio.net.EmotePayloads;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;

/**
 * Joins an <em>unmodded, plain vanilla</em> server and checks the fallback path.
 *
 * <p>The point of this test is the thing that cannot be assumed: a vanilla server discards custom
 * payloads, so the mod has to notice that and switch to the chat bridge. The test asserts the mod
 * really does detect the absence of the protocol, that the emote still animates locally, and that
 * the chat announcement is actually sent — the server's own log then shows it arriving.
 *
 * <p>The address comes from {@code emotestudio.test.server}; the test skips itself when unset, so a
 * normal build does not need a server standing by.
 */
public final class VanillaServerGameTest implements FabricClientGameTest {
    private static final String ADDRESS_PROPERTY = "emotestudio.test.server";
    private static final String EMOTE_ID = "point_up";

    @Override
    public void runTest(ClientGameTestContext context) {
        String address = System.getProperty(ADDRESS_PROPERTY);
        if (address == null || address.isBlank()) {
            return;
        }

        connect(context, address);
        try {
            context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(20);

            assertProtocolUnavailable(context);
            assertEmoteStillPlaysLocally(context);
            assertChatFallbackWasSent(context);

            context.takeScreenshot("emotestudio_vanilla_server");
        } finally {
            context.runOnClient(minecraft ->
                    minecraft.disconnectFromWorld(Component.literal("test finished")));
            context.waitTicks(20);
            // The harness requires each test to hand back a client sitting on the title screen.
            context.setScreen(TitleScreen::new);
            context.waitTicks(5);
        }
    }

    private void connect(ClientGameTestContext context, String address) {
        context.runOnClient(minecraft -> {
            ServerData data = new ServerData("emotestudio-test", address, ServerData.Type.OTHER);
            // The parent screen is only what "back" returns to if the connection fails.
            ConnectScreen.startConnecting(new TitleScreen(), minecraft,
                    ServerAddress.parseString(address), data, false, null);
        });
        // Login, configuration and the first chunks take a while on a cold server.
        context.waitFor(minecraft -> minecraft.player != null && minecraft.level != null, 20 * 60);
        context.waitTicks(40);
    }

    /** A vanilla server never registers our channels, so the mod must fall back rather than shout. */
    private void assertProtocolUnavailable(ClientGameTestContext context) {
        boolean canSend = context.computeOnClient(minecraft ->
                ClientPlayNetworking.canSend(EmotePayloads.PlayEmote.TYPE));
        if (canSend) {
            throw new AssertionError("the test server should not speak the mod's protocol, "
                    + "but the client believes it does");
        }
    }

    private void assertEmoteStillPlaysLocally(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            if (!EmoteStudioClient.playOwnEmote(EMOTE_ID)) {
                throw new AssertionError("could not start '" + EMOTE_ID + "' on a vanilla server");
            }
        });
        context.waitTicks(25);

        float armPitch = context.computeOnClient(minecraft -> {
            ModelPart arm = rightArm(minecraft);
            return arm == null ? Float.NaN : arm.xRot;
        });
        float expected = (float) Math.toRadians(-168.0);
        if (!Float.isFinite(armPitch) || Math.abs(armPitch - expected) > Math.toRadians(8.0)) {
            throw new AssertionError(String.format(
                    "emote did not reach the model on a vanilla server: arm at %.1f deg, expected %.1f",
                    Math.toDegrees(armPitch), Math.toDegrees(expected)));
        }
    }

    /**
     * The chat bridge must have been used. It is enabled by default, and sending is the only way the
     * emote can reach anyone else here — the server log is what proves it arrived.
     */
    private void assertChatFallbackWasSent(ClientGameTestContext context) {
        boolean enabled = context.computeOnClient(minecraft -> EmoteStudioClient.config().chatSync());
        if (!enabled) {
            throw new AssertionError("chat sync should be on by default");
        }
        // Stopping also travels over chat; the rate limiter allows it after the play message.
        context.waitTicks(40);
        context.runOnClient(minecraft -> EmoteStudioClient.stopOwnEmote());
        context.waitTicks(20);
    }

    private static ModelPart rightArm(Minecraft minecraft) {
        if (minecraft.player == null) {
            return null;
        }
        EntityRenderer<?, ?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
        if (!(renderer instanceof LivingEntityRenderer<?, ?, ?> living)) {
            return null;
        }
        if (!(living.getModel() instanceof PlayerModel model)) {
            return null;
        }
        return model.rightArm;
    }
}
