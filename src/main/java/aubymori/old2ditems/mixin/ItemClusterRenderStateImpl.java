package aubymori.old2ditems.mixin;

import aubymori.old2ditems.ObjectWithIdentifier;
import aubymori.old2ditems.ObjectWithItemClusterRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemClusterRenderState.class)
public abstract class ItemClusterRenderStateImpl extends EntityRenderState implements ObjectWithIdentifier {
    @Shadow
    @Final
    public ItemStackRenderState item;
    @Unique
    private Identifier id;

    @Override
    public Identifier o2di$getId() {
        return id;
    }

    @Inject(
        method = "extractItemGroupRenderState",
        at = @At("HEAD")
    )
    private void o2di$getItemId(Entity entity, ItemStack stack, ItemModelResolver itemModelResolver, CallbackInfo ci) {
        this.id = Minecraft.getInstance().player.level().registryAccess().lookup(Registries.ITEM).get().getKey(stack.getItem());
        ((ObjectWithItemClusterRenderState)this.item).o2di$setClusterRenderState((ItemClusterRenderState)(Object)this);
    }
}
