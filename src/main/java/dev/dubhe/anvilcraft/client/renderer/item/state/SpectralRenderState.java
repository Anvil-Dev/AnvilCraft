package dev.dubhe.anvilcraft.client.renderer.item.state;

import dev.dubhe.anvilcraft.mixin.client.ItemStackRenderStateAccessor;
import lombok.Getter;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Getter
public class SpectralRenderState {
    private final ItemStackRenderState ammo = new ItemStackRenderState();

    public SpectralRenderState(ItemStack stack, ItemModelResolver resolver) {
        resolver.updateForTopItem(this.ammo, stack, ItemDisplayContext.GROUND, null, null, 0);
        // 保留三叉戟等武器的平面模型，装填位置只使用发射器提供的变换。
        for (var layer : ((ItemStackRenderStateAccessor) this.ammo).anvilcraft$getLayers()) {
            layer.setItemTransform(ItemTransform.NO_TRANSFORM);
        }
    }
}
