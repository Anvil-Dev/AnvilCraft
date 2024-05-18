package dev.dubhe.anvilcraft.item.enchantment;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FellingEnchantment extends ModEnchantment {
    public FellingEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] applicableSlots) {
        super(rarity, category, applicableSlots);
    }

    @Override
    public boolean canEnchant(@NotNull ItemStack stack) {
        return stack.getItem() instanceof AxeItem;
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
     * 伐木
     *
     * @param level 世界
     * @param pos   位置
     */
    public static void felling(@NotNull Player player, Level level, BlockPos pos, ItemStack tool) {
        if (player.isShiftKeyDown()) return;
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(tool);
        if (!enchantments.containsKey(ModEnchantments.FELLING.get())) return;
        int max = (enchantments.get(ModEnchantments.FELLING.get()) * AnvilCraft.config.fellingBlockPerLevel) - 1;
        while (max > 0) {
            for (BlockPos offset : OFFSET) {
                BlockPos offsetPos = pos.offset(offset);
                BlockState state = level.getBlockState(offsetPos);
                if (!state.is(BlockTags.LOGS)) continue;
                if (max-- < 0) break;
                Block block = state.getBlock();
                boolean bl = level.removeBlock(offsetPos, false);
                if (bl) {
                    block.destroy(level, offsetPos, state);
                }
                if (player.isCreative()) continue;
                BlockEntity entity = level.getBlockEntity(offsetPos);
                block.playerDestroy(level, player, offsetPos, state, entity, tool);
            }
        }
    }
}
