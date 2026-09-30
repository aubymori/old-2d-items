package aubymori.old2ditems.mixin;

import aubymori.old2ditems.ObjectWithBakedQuads;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateImpl implements ObjectWithBakedQuads {
    @Shadow
    private ItemStackRenderState.LayerRenderState[] layers;

    @Override
    public List<BakedQuad>[] o2di$quads() {
        List<BakedQuad>[] la = new List[this.layers.length];
        for (int i = 0; i < la.length; i++) {
            LayerRenderStateAccessor accessor = (LayerRenderStateAccessor)(Object)this.layers[i];
            la[i] = new ArrayList<>(accessor.o2di$getQuads().all());
        }
        return la;
    }

    @Override
    public void o2di$setQuads(List<BakedQuad>[] quads) {
        for (int i = 0; i < this.layers.length; i++) {
            LayerRenderStateAccessor accessor = (LayerRenderStateAccessor)(Object)this.layers[i];
            accessor.o2di$setQuads(ItemQuads.split(quads[i]));
        }
    }
}