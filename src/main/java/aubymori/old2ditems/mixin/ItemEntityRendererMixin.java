package aubymori.old2ditems.mixin;

import aubymori.old2ditems.ObjectWithBakedQuads;
import aubymori.old2ditems.ObjectWithIdentifier;
import aubymori.old2ditems.Old2DItems;
import aubymori.old2ditems.Old2DItemsConfig;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
    @Unique
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

    @Unique
    private ItemEntityRenderState o2di$currentState;

    @Unique
    private CameraRenderState o2di$currentCamera;

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD")
    )
    private void o2di$setRenderState(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        this.o2di$currentState = state;
        this.o2di$currentCamera = camera;
    }

    @ModifyArg(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotate(Lcom/mojang/math/Axis;F)V"
        ),
        index = 1
    )
    private float o2di$setItemRotation(float spin) {
        ItemEntityRenderState state = this.o2di$currentState;
        if (state == null || !shouldChangeItemRotation(state)) {
            return spin;
        }
        CameraRenderState camera = this.o2di$currentCamera;

        if (Old2DItemsConfig.direction == Old2DItemsConfig.Direction.CAMERA_HORZ) {
            Vec3 camPos = camera.pos;
            double dx = camPos.x - state.x;
            double dz = camPos.z - state.z;
            return (float)(0.5 * Math.PI) - (float)Math.atan2(dz, dx);
        } else {
            float degY = 180.0F - camera.yRot;
            return degY * ((float)Math.PI / 180.0F);
        }
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotate(Lcom/mojang/math/Axis;F)V",
            shift = At.Shift.AFTER
        )
    )
    private void o2di$setItemVerticalRotation(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (Old2DItemsConfig.direction == Old2DItemsConfig.Direction.SCREEN && shouldChangeItemRotation(state)) {
            poseStack.rotate(Axis.XN, camera.xRot * ((float)Math.PI / 180.0F));
        }
    }
    @Unique
    private static final RenderPipeline.Snippet ITEM_SNIPPET_NO_CARDINAL_SHADING = RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
        .withVertexShader(Identifier.fromNamespaceAndPath(Old2DItems.MOD_ID,"core/item_no_cardinal_shading"))
        .withFragmentShader("core/item")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2)
        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .buildSnippet();
    @Unique
    private static final RenderPipeline ITEM_CUTOUT_NO_CARDINAL_SHADING = RenderPipelines.register(RenderPipeline.builder(ITEM_SNIPPET_NO_CARDINAL_SHADING)
        .withLocation("pipeline/item_cutout_no_cardinal_shading")
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withColorTargetState(ColorTargetState.DEFAULT)
        .build()
    );
    @Unique
    private static final RenderPipeline ITEM_TRANSLUCENT_NO_CARDINAL_SHADING = RenderPipelines.register(RenderPipeline.builder(ITEM_SNIPPET_NO_CARDINAL_SHADING)
        .withLocation("pipeline/item_translucent_no_cardinal_shading")
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .build()
    );

    @Unique
    private static final Function<Identifier, RenderType> ITEM_CUTOUT_NO_CARDINAL_SHADING_FN;
    @Unique
    private static final Function<Identifier, RenderType> ITEM_TRANSLUCENT_NO_CARDINAL_SHADING_FN;

    static {
        ITEM_CUTOUT_NO_CARDINAL_SHADING_FN = Util.memoize((texture) -> {
            RenderSetup state = RenderSetup.builder(ITEM_CUTOUT_NO_CARDINAL_SHADING).setOitPipelines(RenderPipelines.OIT_ITEM).withTexture("Sampler0", texture).useLightmap().useOverlay().affectsCrumbling().setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).createRenderSetup();
            return RenderType.create("item_cutout_no_cardinal_shading", state);
        });
        ITEM_TRANSLUCENT_NO_CARDINAL_SHADING_FN = Util.memoize((texture) -> {
            RenderSetup state = RenderSetup.builder(ITEM_TRANSLUCENT_NO_CARDINAL_SHADING).setOitPipelines(RenderPipelines.OIT_ITEM).withTexture("Sampler0", texture).useLightmap().affectsCrumbling().sortOnUpload().setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).createRenderSetup();
            return RenderType.create("item_translucent_no_cardinal_shading", state);
        });
    }

    @Unique
    private static RenderType itemCutoutNoCardinalShading(Identifier texture) {
        return ITEM_CUTOUT_NO_CARDINAL_SHADING_FN.apply(texture);
    }

    @Unique
    private static RenderType itemTranslucentNoCardinalShading(Identifier texture) {
        return ITEM_TRANSLUCENT_NO_CARDINAL_SHADING_FN.apply(texture);
    }

    @Unique
    private static final RenderType CUTOUT_BLOCK_ITEM_SHEET_NO_CARDINAL_SHADING = itemCutoutNoCardinalShading(TextureAtlas.LOCATION_BLOCKS);
    @Unique
    private static final RenderType TRANSLUCENT_BLOCK_ITEM_SHEET_NO_CARDINAL_SHADING = itemTranslucentNoCardinalShading(TextureAtlas.LOCATION_BLOCKS);
    @Unique
    private static final RenderType CUTOUT_ITEM_SHEET_NO_CARDINAL_SHADING = itemCutoutNoCardinalShading(TextureAtlas.LOCATION_ITEMS);
    @Unique
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
                    materialInfo.itemGlintRenderType(), materialInfo.itemGlintSpecialRenderType(),
                    materialInfo.tintIndex(), null, materialInfo.lightEmission());

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
        ((ObjectWithBakedQuads)state.item).o2di$setQuads(qls);
    }
}