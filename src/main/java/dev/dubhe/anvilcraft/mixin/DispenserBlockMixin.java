package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {
    @Inject(
        method = "dispenseFrom",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"
        )
    )
    private void dispenseFrom(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci, @Local BlockSource blockSource, @Local DispenserBlockEntity dispenserBlockEntity) {
        BlockPos pos1 = blockSource.pos().relative(state.getValue(DispenserBlock.FACING));
        if (level.getBlockState(pos1).is(ModBlocks.MAGNET_BLOCK)) {
            level.setBlockAndUpdate(pos1, ModBlocks.HOLLOW_MAGNET_BLOCK.getDefaultState());
            dispenserBlockEntity.setItem(0, ModItems.MAGNET_INGOT.asStack());
        }
    }
}
