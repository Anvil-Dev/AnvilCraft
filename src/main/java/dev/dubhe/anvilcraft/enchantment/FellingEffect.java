package dev.dubhe.anvilcraft.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record FellingEffect(int range) implements EnchantmentEntityEffect {

    public static final MapCodec<FellingEffect> CODEC = RecordCodecBuilder.mapCodec(it ->
        it.group(
            Codec.INT.optionalFieldOf("range", 3).forGetter(FellingEffect::range)
        ).apply(it, FellingEffect::new)
    );

    @Override
    public void apply(ServerLevel level, int i, EnchantedItemInUse enchantedItemInUse, Entity entity, Vec3 vec3) {
        if (entity.isShiftKeyDown()) return;
        int max = (i * AnvilCraft.config.fellingBlockPerLevel) + 1;
        if (!(entity instanceof Player player)) return;
        chainMine(
            level,
            player,
            BlockPos.containing(vec3),
            max,
            enchantedItemInUse.itemStack(),
            enchantedItemInUse.onBreak()
        );
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }

    /**
     * 连锁破坏
     *
     * @param level       世界
     * @param sourceBlock 源方块坐标
     * @param max         最大采集数量
     */
    private static void chainMine(ServerLevel level, Player player, BlockPos sourceBlock, int max, ItemStack tool, Consumer<Item> onBreak) {
        BlockPos.breadthFirstTraversal(
            sourceBlock,
            Integer.MAX_VALUE,
            max,
            Util::acceptDirections,
            blockPos -> {
                BlockState blockState = level.getBlockState(blockPos);
                if (blockState.is(BlockTags.LOGS)) {
                    BlockEntity blockEntity = level.getBlockEntity(blockPos);
                    level.removeBlock(blockPos, false);
                    blockState.getBlock().playerDestroy(level, player, blockPos, blockState, blockEntity, tool);
                    if (!sourceBlock.equals(blockPos)) {
                        tool.hurtAndBreak(1, level, player, onBreak);
                    }
                    return true;
                }
                return sourceBlock.equals(blockPos);
            }
        );
    }
}
