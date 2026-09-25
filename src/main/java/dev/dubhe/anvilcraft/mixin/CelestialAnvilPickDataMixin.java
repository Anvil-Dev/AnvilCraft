package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.item.CelestialForgingAnvilBlockItem;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class CelestialAnvilPickDataMixin {
    @WrapOperation(method = "addBlockDataToItem", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/server/level/ServerLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;)"
            + "Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private static @Nullable BlockEntity anvilcraft$pickController(ServerLevel level, BlockPos pos, Operation<BlockEntity> original) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof CelestialForgingAnvilBlock block) return block.getPickBlockEntity(level, pos, state);
        return original.call(level, pos);
    }

    @WrapOperation(method = "addBlockDataToItem", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/block/entity/BlockEntity;saveCustomOnly(Lnet/minecraft/world/level/storage/ValueOutput;)V"))
    private static void anvilcraft$copyAppearance(BlockEntity entity, ValueOutput output, Operation<Void> original) {
        original.call(entity, output);
        if (entity instanceof CelestialForgingAnvilBlockEntity && entity.getLevel() != null && output instanceof TagValueOutput tags) {
            var tag = tags.buildResult().copy();
            CelestialForgingAnvilBlockItem.saveRenderData(tag, entity.getLevel().getGameTime());
            output.store(CelestialForgingAnvilBlockItem.ITEM_RENDER_DATA, CompoundTag.CODEC,
                tag.getCompoundOrEmpty(CelestialForgingAnvilBlockItem.ITEM_RENDER_DATA));
        }
    }
}
