package dev.zihan.emotestudio.mixin;

import dev.zihan.emotestudio.client.anim.EmoteAnimator;
import dev.zihan.emotestudio.client.anim.EmotePoseApplier;
import dev.zihan.emotestudio.emote.EmotePose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overlays the emote pose on top of whatever vanilla animated.
 *
 * <p>Injected at the tail of {@code setupAnim} so the emote wins over the walk, swim and item-use
 * animations, but still blends against them while fading in or out. The sleeve, jacket and trouser
 * overlays are children of the limbs they cover, so they follow along without extra work.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("RETURN"))
    private void emotestudio$applyEmotePose(AvatarRenderState state, CallbackInfo callbackInfo) {
        EmoteAnimator animator = EmoteAnimator.get();
        EmotePose pose = animator.scratchPose();
        float weight = animator.sampleForEntityId(state.id, pose);
        if (weight > 0.0F) {
            EmotePoseApplier.applyToModel((PlayerModel) (Object) this, pose, weight);
        }
    }
}
