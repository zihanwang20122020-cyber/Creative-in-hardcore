package dev.zihan.emotestudio.client;

import dev.zihan.emotestudio.client.anim.EmoteAnimator;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.EmotePart;
import dev.zihan.emotestudio.emote.EmotePose;
import dev.zihan.emotestudio.emote.PartTransform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;

/**
 * Makes emotes visible to players who do not have the mod at all.
 *
 * <p>A vanilla client cannot be told to bend another player's arm — the protocol carries no limb
 * angles, and {@code PlayerModel} recomputes them locally from movement. But a vanilla server
 * <em>does</em> relay four things about you that every vanilla client already knows how to draw:
 * your body rotation, your head rotation, your arm swings, and whether you are crouching or jumping.
 *
 * <p>So this drives those four from the emote's own curves. Everyone on the server — mod or no mod,
 * server modded or not — sees your character turn, look, swing and bob in the emote's rhythm.
 *
 * <p><strong>This one is real movement, not a render trick.</strong> Everything else in the mod only
 * changes how you are drawn; this actually rotates your player and presses jump and sneak. It stays
 * strictly inside what a player could do by hand — no teleporting, no flying, no speed — but it is
 * the one part that a server observes for real, so it only runs when it is the only way anyone else
 * could see anything, and {@code /emote physical off} disables it.
 */
public final class VanillaPerformer {
    /** Ticks between two arm swings, so a fast emote cannot spam the animation packet. */
    private static final int MIN_SWING_INTERVAL = 5;
    /** How far an arm has to travel before its motion is worth a visible swing. */
    private static final float SWING_ANGLE_THRESHOLD = 22.0F;
    /** Root height, in model pixels, above which the emote reads as a hop. */
    private static final float JUMP_THRESHOLD = 5.0F;
    /** Root height below which the emote reads as low to the ground. */
    private static final float CROUCH_THRESHOLD = -2.0F;

    private static final EmotePose POSE = new EmotePose();

    private static Emote active;
    private static float baseYaw;
    private static float basePitch;
    private static float previousArmX;
    private static float previousRootY;
    private static int lastSwingTick;
    private static int tickCounter;
    private static boolean announced;

    private VanillaPerformer() {
    }

    /**
     * Starts performing {@code emote} physically.
     *
     * <p>Called only when the server does not speak the mod's protocol, which is exactly the case
     * where no packet of ours can reach anyone.
     */
    public static void start(Emote emote) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!EmoteStudioClient.config().physicalPerformance() || minecraft.player == null) {
            return;
        }
        active = emote;
        baseYaw = minecraft.player.getYRot();
        basePitch = minecraft.player.getXRot();
        previousArmX = 0.0F;
        previousRootY = 0.0F;
        lastSwingTick = -MIN_SWING_INTERVAL;
        announce(minecraft.player);
    }

    /** Stops performing and hands the camera back to the player. */
    public static void stop() {
        if (active == null) {
            return;
        }
        active = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            // A spin emote would otherwise leave the player facing a random direction.
            minecraft.player.setYRot(baseYaw);
            minecraft.player.setYHeadRot(baseYaw);
            minecraft.player.setXRot(basePitch);
        }
    }

    public static boolean isPerforming() {
        return active != null;
    }

    /**
     * Drives one tick of the physical performance.
     *
     * <p>Everything written here is something the player could do with a mouse and two keys, so the
     * server sees ordinary input rather than anything it would question.
     */
    public static void tick(Minecraft minecraft) {
        tickCounter++;

        LocalPlayer player = minecraft.player;
        if (active == null || player == null) {
            return;
        }
        if (!EmoteStudioClient.config().physicalPerformance()) {
            stop();
            return;
        }
        // The emote may have ended or been cancelled underneath us.
        if (!EmoteAnimator.get().isEmoting(player.getUUID())) {
            stop();
            return;
        }

        float weight = EmoteAnimator.get().samplePose(player.getUUID(),
                EmoteAnimator.get().currentPartialTick(), POSE);
        if (weight <= 0.0F) {
            return;
        }

        PartTransform root = POSE.get(EmotePart.ROOT);
        PartTransform head = POSE.get(EmotePart.HEAD);
        PartTransform body = POSE.get(EmotePart.BODY);
        PartTransform rightArm = POSE.get(EmotePart.RIGHT_ARM);
        PartTransform leftArm = POSE.get(EmotePart.LEFT_ARM);

        applyRotation(player, root, body, head, weight);
        applySwings(player, rightArm, leftArm);
        applyStance(player, root);

        previousRootY = root.animated ? root.posY : 0.0F;
    }

    /**
     * Body and head rotation. This is the channel that carries the most of an emote: a spin, a sway,
     * a nod and a head shake all survive it intact.
     */
    private static void applyRotation(LocalPlayer player, PartTransform root, PartTransform body,
                                      PartTransform head, float weight) {
        float bodyYaw = baseYaw;
        if (root.animated) {
            bodyYaw += root.rotY * weight;
        }
        if (body.animated) {
            bodyYaw += body.rotY * 0.5F * weight;
        }

        float headYaw = bodyYaw;
        float pitch = basePitch;
        if (head.animated) {
            headYaw += head.rotY * weight;
            // Head pitch is inverted between model space and look direction.
            pitch = Math.clamp(basePitch + head.rotX * weight, -90.0F, 90.0F);
        }

        player.setYRot(bodyYaw);
        player.setYHeadRot(headYaw);
        player.setXRot(pitch);
    }

    /**
     * Turns arm motion into swing animations. A swing fires when an arm reaches the far end of its
     * travel, which lines up with the beat of a wave, a clap or a punch.
     */
    private static void applySwings(LocalPlayer player, PartTransform rightArm, PartTransform leftArm) {
        if (!rightArm.animated && !leftArm.animated) {
            return;
        }
        float armX = rightArm.animated ? rightArm.rotX : leftArm.rotX;
        float delta = armX - previousArmX;
        boolean reversed = delta > 0.0F && previousArmX < -SWING_ANGLE_THRESHOLD;

        if (reversed && tickCounter - lastSwingTick >= MIN_SWING_INTERVAL) {
            lastSwingTick = tickCounter;
            player.swing(InteractionHand.MAIN_HAND);
        }
        previousArmX = armX;
    }

    /**
     * Crouching and hopping. Both are ordinary inputs, and both are relayed to every client, so a
     * low pose reads as a crouch and a bouncy emote reads as bouncing.
     */
    private static void applyStance(LocalPlayer player, PartTransform root) {
        if (!root.animated) {
            return;
        }
        boolean crouch = root.posY <= CROUCH_THRESHOLD;
        boolean rising = root.posY - previousRootY > 0.6F && root.posY > JUMP_THRESHOLD;
        boolean jump = rising && player.onGround();

        if (!crouch && !jump) {
            return;
        }
        Input current = player.input.keyPresses;
        player.input.keyPresses = new Input(
                current.forward(), current.backward(), current.left(), current.right(),
                jump || current.jump(), crouch || current.shift(), current.sprint());
    }

    private static void announce(LocalPlayer player) {
        if (announced) {
            return;
        }
        announced = true;
        player.sendSystemMessage(Component
                .translatable("message.emotestudio.physical_notice")
                .withStyle(ChatFormatting.GRAY));
    }
}
