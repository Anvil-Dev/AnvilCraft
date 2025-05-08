package dev.dubhe.anvilcraft.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record HarvestEffect(int range) implements EnchantmentEntityEffect {
    private static final List<BlockPos> ITERATING_OFFSET = new ArrayList<>();

    static {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ITERATING_OFFSET.add(new BlockPos(dx, 0, dz));
            }
        }
    }

    public static final MapCodec<HarvestEffect> CODEC = RecordCodecBuilder.mapCodec(it ->
        it.group(
            Codec.INT.optionalFieldOf("range", 3)
                .forGetter(HarvestEffect::range)
        ).apply(it, HarvestEffect::new)
    );

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse enchantedItemInUse, Entity owner, Vec3 vec3) {
        ItemStack tool = enchantedItemInUse.itemStack();
        BlockPos pos = BlockPos.containing(vec3);
        if (!(owner instanceof Player player)) return;
        BlockState targetBlockState = level.getBlockState(pos);
        boolean hasValidCropBlock = false;
        if (targetBlockState.getBlock() instanceof CropBlock cropBlock) {
            if (cropBlock.isMaxAge(targetBlockState)) {
                hasValidCropBlock = true;
                BlockEntity entity = level.getBlockEntity(pos);
                cropBlock.playerWillDestroy(level, pos, targetBlockState, player);
                cropBlock.playerDestroy(level, player, pos, targetBlockState, entity, tool);
                level.setBlockAndUpdate(pos, cropBlock.getStateForAge(0));
                tool.hurtAndBreak(1, level, player, enchantedItemInUse.onBreak());
            }
        } else return;
        int max = enchantmentLevel * 2 + 1;
        max *= max;
        if (hasValidCropBlock) max -= 1;
        Deque<BlockPos> iteratingPos = new ArrayDeque<>();
        iteratingPos.push(pos);
        List<BlockPos> vis = new ArrayList<>();
        while (!iteratingPos.isEmpty() && max >= 0) {
            BlockPos currentIterating = iteratingPos.pop();
            if (vis.contains(currentIterating)) continue;
            vis.add(currentIterating);
            max--;
            for (BlockPos offset : ITERATING_OFFSET) {
                BlockPos currentPos = currentIterating.offset(offset);
                iteratingPos.add(currentPos);
            }
            BlockState state = level.getBlockState(currentIterating);
            if (!(state.getBlock() instanceof CropBlock block)) continue;
            if (!block.isMaxAge(state)) continue;
            BlockState newState = block.getStateForAge(0);
            cropBlock.playerWillDestroy(level, currentIterating, state, player);
            cropBlock.playerDestroy(level, player, currentIterating, state, level.getBlockEntity(currentIterating), tool);
            level.setBlockAndUpdate(currentIterating, newState);
            tool.hurtAndBreak(1, level, player, enchantedItemInUse.onBreak());
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}
