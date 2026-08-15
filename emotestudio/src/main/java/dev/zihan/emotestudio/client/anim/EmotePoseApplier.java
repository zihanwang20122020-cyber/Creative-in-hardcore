package dev.zihan.emotestudio.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.zihan.emotestudio.emote.EmotePart;
import dev.zihan.emotestudio.emote.EmotePose;
import dev.zihan.emotestudio.emote.PartTransform;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Quaternionf;

/**
 * Pushes a sampled {@link EmotePose} onto the live player model.
 *
 * <p>Emote values are authored relative to each part's rest pose, and blended against whatever
 * vanilla just computed. That is what lets an emote fade in over a walk cycle instead of snapping.
 *
 * <p>Authoring convention: <em>+Y is up</em> everywhere. Model space actually points +Y downwards,
 * so the vertical offset is negated here rather than making every emote file counter-intuitive.
 */
public final class EmotePoseApplier {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    /** Model pixels per block, used to convert root offsets into world units. */
    private static final float PIXELS_PER_BLOCK = 16.0F;

    private EmotePoseApplier() {
    }

    /** Applies the six limb tracks. The root track is handled separately by the renderer mixin. */
    public static void applyToModel(HumanoidModel<?> model, EmotePose pose, float weight) {
        if (weight <= 0.0F) {
            return;
        }
        float w = Math.min(weight, 1.0F);
        for (EmotePart part : EmotePart.MODEL_PARTS) {
            PartTransform transform = pose.get(part);
            if (!transform.animated) {
                continue;
            }
            ModelPart modelPart = modelPartFor(model, part);
            if (modelPart != null) {
                applyPart(modelPart, transform, w);
            }
        }
    }

    private static ModelPart modelPartFor(HumanoidModel<?> model, EmotePart part) {
        return switch (part) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
            case ROOT -> null;
        };
    }

    private static void applyPart(ModelPart part, PartTransform transform, float weight) {
        PartPose rest = part.getInitialPose();

        part.xRot = lerp(part.xRot, rest.xRot() + transform.rotX * DEG_TO_RAD, weight);
        part.yRot = lerp(part.yRot, rest.yRot() + transform.rotY * DEG_TO_RAD, weight);
        part.zRot = lerp(part.zRot, rest.zRot() + transform.rotZ * DEG_TO_RAD, weight);

        part.x = lerp(part.x, rest.x() + transform.posX, weight);
        part.y = lerp(part.y, rest.y() - transform.posY, weight);
        part.z = lerp(part.z, rest.z() + transform.posZ, weight);

        part.xScale = lerp(part.xScale, rest.xScale() * transform.scaleX, weight);
        part.yScale = lerp(part.yScale, rest.yScale() * transform.scaleY, weight);
        part.zScale = lerp(part.zScale, rest.zScale() * transform.scaleZ, weight);
    }

    /**
     * Applies the root track: offsets and spins the whole avatar.
     *
     * <p>Must be called while the pose stack is still in entity space (+Y up, one unit per block),
     * which is the state right after vanilla's {@code setupRotations}.
     */
    public static void applyRoot(PoseStack poseStack, EmotePose pose, float weight) {
        PartTransform root = pose.get(EmotePart.ROOT);
        if (!root.animated || weight <= 0.0F) {
            return;
        }
        float w = Math.min(weight, 1.0F);

        poseStack.translate(
                (root.posX * w) / PIXELS_PER_BLOCK,
                (root.posY * w) / PIXELS_PER_BLOCK,
                (root.posZ * w) / PIXELS_PER_BLOCK);

        if (root.rotX != 0.0F || root.rotY != 0.0F || root.rotZ != 0.0F) {
            Quaternionf rotation = new Quaternionf().rotationYXZ(
                    root.rotY * w * DEG_TO_RAD,
                    root.rotX * w * DEG_TO_RAD,
                    root.rotZ * w * DEG_TO_RAD);
            poseStack.mulPose(rotation);
        }

        if (root.scaleX != 1.0F || root.scaleY != 1.0F || root.scaleZ != 1.0F) {
            poseStack.scale(
                    lerp(1.0F, root.scaleX, w),
                    lerp(1.0F, root.scaleY, w),
                    lerp(1.0F, root.scaleZ, w));
        }
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
