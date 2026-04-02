package aubymori.old2ditems.mixin;

import aubymori.old2ditems.ObjectWithBakedQuads;
import aubymori.old2ditems.ObjectWithIdentifier;
import aubymori.old2ditems.ObjectWithItemClusterRenderState;
import aubymori.old2ditems.Old2DItemsConfig;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements ObjectWithBakedQuads, ObjectWithItemClusterRenderState {
    private ItemClusterRenderState renderState;

    @Shadow private ItemStackRenderState.LayerRenderState[] layers;
    @Shadow private int activeLayerCount;

    @Shadow
    public abstract boolean usesBlockLight();

    @Shadow
    private ItemDisplayContext displayContext;
    private static final RenderPipeline ITEM_ENTITY_NO_CARDINAL_SHADING = RenderPipelines.register(RenderPipeline.builder(new RenderPipeline.Snippet[] { RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET
    }).withLocation("pipeline/item_entity_translucent_cull")
        .withVertexShader(Identifier.fromNamespaceAndPath("old-2d-items", "core/rendertype_item_entity_no_cardinal_shading"))
        .withFragmentShader("core/rendertype_item_entity_translucent_cull")
        .withSampler("Sampler0")
        .withSampler("Sampler2")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
        .build());

    @Inject(
        method = "submit",
        at = @At("HEAD")
    )
    private void o2di$changeRenderType(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, int k, CallbackInfo ci) {
        if (!Old2DItemsConfig.flatModels || this.usesBlockLight() || this.displayContext != ItemDisplayContext.GROUND || Old2DItemsConfig.exceptions.contains(((ObjectWithIdentifier)this.renderState).o2di$getId())) {
            return;
        }

        RenderSetup state = RenderSetup.builder(ITEM_ENTITY_NO_CARDINAL_SHADING)
            .withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();

        RenderType renderType = RenderType.create("item_entity_no_cardinal_shading", state);

        for (int l = 0; l < activeLayerCount; l++) {
            this.layers[l].setRenderType(renderType);
        }
    }

    @Override
    public List<BakedQuad>[] o2di$quads() {
        List<BakedQuad>[] la = new List[this.layers.length];
        for (int i = 0; i < la.length; i++) {
            la[i] = this.layers[i].prepareQuadList();
        }
        return la;
    }

    @Override
    public void o2di$setClusterRenderState(ItemClusterRenderState state) {
        this.renderState = state;
    }
}
