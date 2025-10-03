package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.dubhe.anvilcraft.block.MagnetBlock.LIT;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {
    @Inject(
        method = "dispenseFrom",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"
        )
    )
    private void dispenseFrom(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        BlockPos pos1 = pos.relative(state.getValue(DispenserBlock.FACING));
        if (level.getBlockState(pos1).is(ModBlocks.MAGNET_BLOCK)) {
            BlockState blockState = ModBlocks.HOLLOW_MAGNET_BLOCK.get().defaultBlockState();
            if (blockState.hasProperty(LIT)) {
                blockState = blockState.setValue(LIT, level.hasNeighborSignal(pos1));
            }
            level.setBlockAndUpdate(pos1, blockState);
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof DispenserBlockEntity dispenser)) return;
            dispenser.setItem(0, ModItems.MAGNET_INGOT.asStack());
        }
    }
}
