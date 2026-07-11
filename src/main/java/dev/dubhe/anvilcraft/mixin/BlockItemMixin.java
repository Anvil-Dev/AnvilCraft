package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.util.BlockItemPlacementStateOverride;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @WrapOperation(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"
        )
    )
    private @Nullable BlockState anvilcraft$overridePlacementState(
        BlockItem instance,
        BlockPlaceContext context,
        Operation<BlockState> original
    ) {
        BlockState override = BlockItemPlacementStateOverride.get();
        return override == null ? original.call(instance, context) : override;
    }
}
