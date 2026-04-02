package aubymori.old2ditems.mixin;

import aubymori.old2ditems.ObjectWithBakedQuads;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateImpl implements ObjectWithBakedQuads {
    @Shadow private ItemStackRenderState.LayerRenderState[] layers;

    public List<BakedQuad>[] o2di$quads() {
        List<BakedQuad>[] la = new List[this.layers.length];
        for (int i = 0; i < la.length; i++) {
            la[i] = this.layers[i].prepareQuadList();
        }
        return la;
    }
}
