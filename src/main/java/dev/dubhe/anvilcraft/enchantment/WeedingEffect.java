package dev.dubhe.anvilcraft.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record WeedingEffect(int range) implements EnchantmentEntityEffect {
    public static final MapCodec<WeedingEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.optionalFieldOf("range", 3).forGetter(WeedingEffect::range)
    ).apply(instance, WeedingEffect::new));

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        BlockPos pos = BlockPos.containing(origin);
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.SHORT_GRASS) && !state.is(Blocks.TALL_GRASS)) {
            return;
        }
        int r = Math.min(enchantmentLevel, 7);
        for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(r, 0, r), pos.offset(-r, 0, -r))) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.is(Blocks.SHORT_GRASS) || blockState.is(Blocks.TALL_GRASS)) {
                level.destroyBlock(blockPos, true);
            }
        }
    }
}
