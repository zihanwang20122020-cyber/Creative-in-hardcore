package dev.zihan.emotestudio.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.zihan.emotestudio.client.anim.EmoteAnimator;
import dev.zihan.emotestudio.client.anim.EmotePoseApplier;
import dev.zihan.emotestudio.emote.EmotePose;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the {@code root} track, which moves and spins the entire avatar.
 *
 * <p>{@code setupRotations} returns while the pose stack is still in entity space: +Y up, one unit
 * per block, before vanilla flips it into model space. That makes it the one place where a root
 * offset means the same thing an emote author expects it to mean.
 *
 * <p>Only the drawing is affected — the player's real position never changes, which is why this is
 * safe to use on your own world or a LAN world.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(
            method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("RETURN"))
    private void emotestudio$applyRootMotion(
            AvatarRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo callbackInfo) {
        EmoteAnimator animator = EmoteAnimator.get();
        EmotePose pose = animator.scratchPose();
        float weight = animator.sampleForEntityId(state.id, pose);
        if (weight > 0.0F) {
            EmotePoseApplier.applyRoot(poseStack, pose, weight);
        }
    }
}
