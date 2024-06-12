package dev.dubhe.anvilcraft.item.enchantment;

import dev.dubhe.anvilcraft.init.ModEnchantments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HarvestEnchantment extends ModEnchantment {
    public HarvestEnchantment(
            Rarity rarity, EnchantmentCategory category, EquipmentSlot[] applicableSlots) {
        super(rarity, category, applicableSlots);
    }

    @Override
    public boolean canEnchant(@NotNull ItemStack stack) {
        return stack.getItem() instanceof HoeItem;
    }

    @Override
    public int getMinCost(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 15;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    /**
     * 使该附魔无法从村民交易附魔书获得
     */
    @Override
    public boolean isTradeable() {
        return false;
    }

    /**
     * 收割
     */
    public static void harvest(
            @NotNull Player player, @NotNull Level level, BlockPos pos, ItemStack tool) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(tool);
        if (!enchantments.containsKey(ModEnchantments.HARVEST.get())) return;
        BlockState state0 = level.getBlockState(pos);
        boolean flag = false;
        if (state0.getBlock() instanceof CropBlock cropBlock) {
            if (cropBlock.isMaxAge(state0)) {
                flag = true;
                BlockEntity entity = level.getBlockEntity(pos);
                cropBlock.playerWillDestroy(level, pos, state0, player);
                cropBlock.playerDestroy(level, player, pos, state0, entity, tool);
                level.setBlockAndUpdate(pos, cropBlock.getStateForAge(0));
            }
        } else return;
        int max = (int) Math.pow(enchantments.get(ModEnchantments.HARVEST.get()) * 2 + 1, 2);
        if (flag) max -= 1;
        List<BlockPos> posList = new ArrayList<>();
        posList.add(pos);
        felling:
        while (!posList.isEmpty()) {
            Iterator<BlockPos> iterator = posList.iterator();
            List<BlockPos> posList2 = new ArrayList<>();
            while (iterator.hasNext()) {
                BlockPos next = iterator.next();
                for (BlockPos offset : OFFSET) {
                    BlockPos nextOffset = next.offset(offset);
                    BlockState state = level.getBlockState(nextOffset);
                    if (!(state.getBlock() instanceof CropBlock block)) continue;
                    if (max-- <= 0) break felling;
                    posList2.add(nextOffset);
                    if (!block.isMaxAge(state)) continue;
                    BlockEntity entity = level.getBlockEntity(nextOffset);
                    block.playerWillDestroy(level, nextOffset, state, player);
                    block.playerDestroy(level, player, nextOffset, state, entity, tool);
                    level.setBlockAndUpdate(nextOffset, block.getStateForAge(0));
                }
                iterator.remove();
            }
            posList.addAll(posList2);
        }
    }
}
