package dev.zihan.emotestudio.clienttest;

import dev.zihan.emotestudio.client.EmoteStudioClient;
import dev.zihan.emotestudio.client.anim.EmoteAnimator;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.io.EmoteRepository;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;

/**
 * Boots a real client, loads a world, and checks that emotes reach the rendered player model.
 *
 * <p>This is the check the unit tests cannot make: the sampler could be perfect and the mixins still
 * never fire. Here the assertions read the live {@link PlayerModel} after frames have actually been
 * drawn, so a broken injection point fails the build instead of shipping.
 */
public final class EmoteRenderingGameTest implements FabricClientGameTest {
    /** Rotations are radians on the model; a degree of slack absorbs blend-in timing. */
    private static final float TOLERANCE = (float) Math.toRadians(6.0);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitTicks(20);

            context.runOnClient(minecraft -> {
                // The local player is only rendered in third person, which is where emotes show.
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            });
            context.waitTicks(10);

            assertRepositoryLoaded(context);
            float restingArm = armPitch(context, "resting pose");

            testLimbAnimationReachesModel(context, restingArm);
            context.takeScreenshot("emotestudio_pointing_up");

            testRootTrackReachesRenderer(context);
            context.takeScreenshot("emotestudio_floating");

            testStopReturnsToRest(context, restingArm);
            context.takeScreenshot("emotestudio_back_to_rest");
        }
    }

    private void assertRepositoryLoaded(ClientGameTestContext context) {
        int count = context.computeOnClient(minecraft -> EmoteRepository.get().all().size());
        if (count < 200) {
            throw new AssertionError("expected at least 200 built-in emotes, found " + count);
        }
        Emote moonwalk = context.computeOnClient(minecraft -> EmoteRepository.get().byId("moonwalk"));
        if (moonwalk == null || moonwalk.keyframeCount() < 20) {
            throw new AssertionError("moonwalk is missing or has too few keyframes");
        }
    }

    /**
     * Plays an emote whose right arm is pinned high and confirms the model actually holds that
     * angle — proof that the PlayerModel injection ran and won over the vanilla pose.
     */
    private void testLimbAnimationReachesModel(ClientGameTestContext context, float restingArm) {
        context.runOnClient(minecraft -> {
            if (!EmoteStudioClient.playOwnEmote("point_up")) {
                throw new AssertionError("could not start the 'point_up' emote");
            }
        });
        // Long enough for the blend-in to finish.
        context.waitTicks(20);

        float raised = armPitch(context, "point_up");

        // point_up pins the right arm at -168 degrees, far from any resting or walking pose.
        float expected = (float) Math.toRadians(-168.0);
        if (Math.abs(raised - expected) > TOLERANCE) {
            throw new AssertionError(String.format(
                    "right arm should be held at %.1f deg while pointing up, but the model reads %.1f deg",
                    Math.toDegrees(expected), Math.toDegrees(raised)));
        }
        if (Math.abs(raised - restingArm) < Math.toRadians(45.0)) {
            throw new AssertionError("the emote pose is indistinguishable from the resting pose");
        }
    }

    /** A root-track emote must move the whole avatar, which is the renderer injection's job. */
    private void testRootTrackReachesRenderer(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            if (!EmoteStudioClient.playOwnEmote("float")) {
                throw new AssertionError("could not start the 'float' emote");
            }
        });
        context.waitTicks(30);

        boolean animating = context.computeOnClient(minecraft ->
                EmoteAnimator.get().isEmoting(minecraft.player.getUUID()));
        if (!animating) {
            throw new AssertionError("the float emote stopped before it should have");
        }

        // Sampling the root track directly confirms the data the renderer mixin consumes.
        float rootLift = context.computeOnClient(minecraft -> {
            var pose = new dev.zihan.emotestudio.emote.EmotePose();
            float weight = EmoteAnimator.get().samplePose(minecraft.player.getUUID(), 0.0F, pose);
            if (weight <= 0.0F) {
                throw new AssertionError("float produced no blend weight");
            }
            return pose.get(dev.zihan.emotestudio.emote.EmotePart.ROOT).posY;
        });
        if (rootLift < 1.0F) {
            throw new AssertionError("float should lift the avatar, root offset was " + rootLift);
        }
    }

    /** Stopping must hand the model back to vanilla rather than leaving the limb stuck. */
    private void testStopReturnsToRest(ClientGameTestContext context, float restingArm) {
        context.runOnClient(minecraft -> EmoteStudioClient.stopOwnEmote());
        context.waitTicks(40);

        float afterStop = armPitch(context, "after stop");
        if (Math.abs(afterStop - restingArm) > Math.toRadians(25.0)) {
            throw new AssertionError(String.format(
                    "arm did not return to rest after stopping: %.1f deg vs %.1f deg",
                    Math.toDegrees(afterStop), Math.toDegrees(restingArm)));
        }
    }

    /** Reads the right arm's pitch straight off the model the renderer just used. */
    private float armPitch(ClientGameTestContext context, String what) {
        Float value = context.computeOnClient(minecraft -> {
            ModelPart arm = rightArm(minecraft);
            return arm == null ? null : arm.xRot;
        });
        if (value == null) {
            throw new AssertionError("could not reach the player model while checking " + what);
        }
        return value;
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
