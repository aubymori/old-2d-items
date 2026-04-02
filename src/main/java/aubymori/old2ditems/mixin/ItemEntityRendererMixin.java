package aubymori.old2ditems.mixin;

import aubymori.old2ditems.ObjectWithBakedQuads;
import aubymori.old2ditems.ObjectWithIdentifier;
import aubymori.old2ditems.Old2DItems;
import aubymori.old2ditems.Old2DItemsConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    private boolean shouldChangeItemRotation(ItemEntityRenderState state) {
        if (Old2DItemsConfig.direction == Old2DItemsConfig.Direction.SPIN) {
            return false;
        }

        if (!Old2DItemsConfig.affect3DModels && state.item.usesBlockLight()) {
            return false;
        }

        ObjectWithIdentifier idObj = (ObjectWithIdentifier)state;
        Identifier id = idObj.o2di$getId();
        return !Old2DItemsConfig.exceptions.contains(id);
    }

    @ModifyExpressionValue(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/math/Axis;rotation(F)Lorg/joml/Quaternionf;"
        )
    )
    private Quaternionf o2di$setItemRotation(Quaternionf quaternion, ItemEntityRenderState state) {
        if (shouldChangeItemRotation(state)) {
            Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            Quaternionf q = new Quaternionf();

            if (Old2DItemsConfig.direction == Old2DItemsConfig.Direction.CAMERA_HORZ) {
                Vec3 camPos = cam.position();
                double dx = camPos.x - state.x;
                double dz = camPos.z - state.z;
                return q.rotationY((float)(.5 * Math.PI) - (float)Math.atan2(dz, dx));
            } else {
                float degY = 180.0F - cam.yRot();
                return q.rotationY(degY * ((float)Math.PI / 180.0F));
            }
        }
        return quaternion;
    }

    @Inject(
        method = "submit",
        at = @At(
            shift = At.Shift.AFTER,
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"
        )
    )
    private void o2di$setItemVerticalRotation(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (Old2DItemsConfig.direction == Old2DItemsConfig.Direction.SCREEN && shouldChangeItemRotation(state)) {
            poseStack.mulPose(Axis.XN.rotationDegrees(Minecraft.getInstance().gameRenderer.getMainCamera().xRot()));
        }
    }

    @Inject(
        method = "submit",
        at = @At(
            shift = At.Shift.AFTER,
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"
        )
    )
    private void o2di$flattenItem(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (!Old2DItemsConfig.flatModels || state.item.usesBlockLight()) {
            return;
        }

        Identifier id = ((ObjectWithIdentifier)state).o2di$getId();
        if (Old2DItemsConfig.exceptions.contains(id)) {
            return;
        }

        List<BakedQuad>[] qls = ((ObjectWithBakedQuads)state.item).o2di$quads();
        for (final List<BakedQuad> ql : qls) {
            ql.removeIf(q -> q.direction() != Direction.SOUTH);

            // Invert the item's quads for the back side. It is impossible to see the backside with
            // any mode except "Screen, horizontal", and "Spin".
            if (Old2DItemsConfig.direction == Old2DItemsConfig.Direction.SPIN ||
                (Old2DItemsConfig.direction == Old2DItemsConfig.Direction.SCREEN_HORZ && Old2DItemsConfig.renderBack)) {
                int length = ql.size();
                for (int i = 0; i < length; i++) {
                    BakedQuad q = ql.get(i);
                    BakedQuad nq = new BakedQuad(
                        q.position0(), q.position3(), q.position2(), q.position1(),
                        q.packedUV0(), q.packedUV3(), q.packedUV2(), q.packedUV1(),
                        q.tintIndex(), Direction.NORTH, q.sprite(), q.shade(), q.lightEmission());
                    ql.add(nq);
                }
            }
        }
    }
}
