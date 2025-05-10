package dev.dubhe.anvilcraft.mixin.compat;

import appeng.items.misc.WrappedGenericStack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.BatchCrafterBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BatchCrafterBlock.class)
public class BatchCrafterBlockMixin {
    @WrapOperation(
        method = "onRemove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"
        )
    )
    void avoidDropWrappedGenericStack(
        Level level,
        double x,
        double y,
        double z,
        ItemStack itemStack,
        Operation<Void> original
    ) {
        if (itemStack.getItem() instanceof WrappedGenericStack item) {
            return;
        }
        original.call(level, x, y, z, itemStack);
    }
}
