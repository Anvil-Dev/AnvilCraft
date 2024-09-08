package dev.dubhe.anvilcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net/minecraft/core/dispenser/DispenseItemBehavior$27")
abstract class DispenseItemWaterBottleBehaviorMixin extends DefaultDispenseItemBehavior {
    @Inject(
        method = "execute(Lnet/minecraft/core/BlockSource;"
            + "Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;"
                + "getBlockState(Lnet/minecraft/core/BlockPos;)"
                + "Lnet/minecraft/world/level/block/state/BlockState;"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true
    )
    public void takeLiquidFromCauldron(
        BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir, @NotNull ServerLevel serverLevel,
        BlockPos blockPos, BlockPos blockPos2
    ) {
        BlockState state = serverLevel.getBlockState(blockPos2);
        if (state.is(Blocks.CAULDRON)) {
            serverLevel.setBlockAndUpdate(blockPos2, Blocks.WATER_CAULDRON.defaultBlockState());
            cir.setReturnValue(new ItemStack(Items.GLASS_BOTTLE));
            return;
        }
        if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) < 3) {
            serverLevel.setBlockAndUpdate(blockPos2, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL) + 1));
            cir.setReturnValue(new ItemStack(Items.GLASS_BOTTLE));
        }
    }
}
