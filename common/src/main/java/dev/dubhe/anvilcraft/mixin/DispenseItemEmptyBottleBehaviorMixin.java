package dev.dubhe.anvilcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/core/dispenser/DispenseItemBehavior$24")
abstract class DispenseItemEmptyBottleBehaviorMixin extends OptionalDispenseItemBehavior {
    @Shadow
    protected abstract ItemStack takeLiquid(BlockSource source, ItemStack empty, ItemStack filled);

    @Inject(
        method = "execute(Lnet/minecraft/core/BlockSource;Lnet/minecraft/world/item/ItemStack;)"
            + "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/dispenser/OptionalDispenseItemBehavior;"
                + "execute(Lnet/minecraft/core/BlockSource;Lnet/minecraft/world/item/ItemStack;)"
                + "Lnet/minecraft/world/item/ItemStack;"
        ),
        cancellable = true
    )
    public void takeLiquidFromCauldron(
        @NotNull BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir
    ) {
        ServerLevel serverLevel = source.getLevel();
        BlockPos blockPos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
        BlockState state = serverLevel.getBlockState(blockPos);
        if (state.is(Blocks.WATER_CAULDRON)) {
            this.setSuccess(true);
            LayeredCauldronBlock.lowerFillLevel(state, serverLevel, blockPos);
            cir.setReturnValue(
                this.takeLiquid(source, stack, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER))
            );
        }
    }
}
