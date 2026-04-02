package aubymori.old2ditems.mixin;

import aubymori.old2ditems.ObjectWithBakedQuads;
import aubymori.old2ditems.ObjectWithIdentifier;
import aubymori.old2ditems.Old2DItems;
import aubymori.old2ditems.Old2DItemsConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static net.minecraft.client.renderer.RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET;

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

    private static final RenderPipeline.Snippet ITEM_SNIPPET_NO_CARDINAL_SHADING = RenderPipeline.builder(new RenderPipeline.Snippet[]{ MATRICES_FOG_LIGHT_DIR_SNIPPET
    }).withVertexShader(Identifier.fromNamespaceAndPath(Old2DItems.MOD_ID, "core/item_no_cardinal_shading"))
        .withFragmentShader("core/item")
        .withSampler("Sampler0")
        .withSampler("Sampler2")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .buildSnippet();

    private static final RenderPipeline ITEM_CUTOUT_NO_CARDINAL_SHADING = RenderPipelines.register(RenderPipeline.builder(new RenderPipeline.Snippet[]{ ITEM_SNIPPET_NO_CARDINAL_SHADING
    }).withLocation("pipeline/item_cutout")
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .build()
    );

    private static final RenderPipeline ITEM_TRANSLUCENT_NO_CARDINAL_SHADING = RenderPipelines.register(RenderPipeline.builder(new RenderPipeline.Snippet[]{ ITEM_SNIPPET_NO_CARDINAL_SHADING
    }).withLocation("pipeline/item_translucent")
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .build()
    );

    private static final Function<Identifier, RenderType> ITEM_CUTOUT_NO_CARDINAL_SHADING_FN;
    private static final Function<Identifier, RenderType> ITEM_TRANSLUCENT_NO_CARDINAL_SHADING_FN;

    static {
        ITEM_CUTOUT_NO_CARDINAL_SHADING_FN = Util.memoize((texture) -> {
            RenderSetup state = RenderSetup.builder(ITEM_CUTOUT_NO_CARDINAL_SHADING).withTexture("Sampler0", texture).useLightmap().affectsCrumbling().setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).createRenderSetup();
            return RenderType.create("item_cutout_no_cardinal_shading", state);
        });
        ITEM_TRANSLUCENT_NO_CARDINAL_SHADING_FN = Util.memoize((texture) -> {
            RenderSetup state = RenderSetup.builder(ITEM_TRANSLUCENT_NO_CARDINAL_SHADING).withTexture("Sampler0", texture).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).useLightmap().affectsCrumbling().sortOnUpload().setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).createRenderSetup();
            return RenderType.create("item_translucent_no_cardinal_shading", state);
        });
    }

    private static RenderType itemCutoutNoCardinalShading(Identifier texture) {
        return ITEM_CUTOUT_NO_CARDINAL_SHADING_FN.apply(texture);
    }

    private static RenderType itemTranslucentNoCardinalShading(Identifier texture) {
        return ITEM_TRANSLUCENT_NO_CARDINAL_SHADING_FN.apply(texture);
    }

    private static final RenderType CUTOUT_BLOCK_ITEM_SHEET_NO_CARDINAL_SHADING = itemCutoutNoCardinalShading(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType TRANSLUCENT_BLOCK_ITEM_SHEET_NO_CARDINAL_SHADING = itemTranslucentNoCardinalShading(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType CUTOUT_ITEM_SHEET_NO_CARDINAL_SHADING = itemCutoutNoCardinalShading(TextureAtlas.LOCATION_ITEMS);
    private static final RenderType TRANSLUCENT_ITEM_SHEET_NO_CARDINAL_SHADING = itemTranslucentNoCardinalShading(TextureAtlas.LOCATION_ITEMS);

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
        int length = qls.length;
        for (int i = 0; i < length; i++) {
            List<BakedQuad> ql = qls[i];
            ql.removeIf(q -> q.direction() != Direction.SOUTH);

            List<BakedQuad> oldQl = new ArrayList<>(ql);
            ql.clear();

            boolean renderBack = Old2DItemsConfig.direction == Old2DItemsConfig.Direction.SPIN ||
                (Old2DItemsConfig.direction == Old2DItemsConfig.Direction.SCREEN_HORZ && Old2DItemsConfig.renderBack);
            int qlLength = oldQl.size();
            for (int j = 0; j < qlLength; j++) {
                BakedQuad quad = oldQl.get(j);
                BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
                RenderType itemRenderType;
                if (materialInfo.sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
                    itemRenderType = materialInfo.sprite().transparency().hasTranslucent() ? TRANSLUCENT_BLOCK_ITEM_SHEET_NO_CARDINAL_SHADING : CUTOUT_BLOCK_ITEM_SHEET_NO_CARDINAL_SHADING;
                } else {
                    itemRenderType = materialInfo.sprite().transparency().hasTranslucent() ? TRANSLUCENT_ITEM_SHEET_NO_CARDINAL_SHADING : CUTOUT_ITEM_SHEET_NO_CARDINAL_SHADING;
                }

                BakedQuad.MaterialInfo newMaterialInfo = new BakedQuad.MaterialInfo(
                    materialInfo.sprite(), materialInfo.layer(), itemRenderType,
                    materialInfo.tintIndex(), materialInfo.shade(), materialInfo.lightEmission());

                BakedQuad newQuad = new BakedQuad(
                    quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                    quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                    quad.direction(), newMaterialInfo);
                ql.add(newQuad);

                if (renderBack) {
                    BakedQuad newBackQuad = new BakedQuad(
                        quad.position0(), quad.position3(), quad.position2(), quad.position1(),
                        quad.packedUV0(), quad.packedUV3(), quad.packedUV2(), quad.packedUV1(),
                        Direction.NORTH, newMaterialInfo);
                    ql.add(newBackQuad);
                }
            }
        }
    }
}
