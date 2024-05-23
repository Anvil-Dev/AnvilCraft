package dev.dubhe.anvilcraft.item.enchantment;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

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
        FellingEnchantment.chainMine(level, pos, max);
    }

    /**
     * 连锁破坏原木
     *
     * @param level       世界
     * @param sourceBlock 源方块坐标
     * @param max         最大采集数量
     */
    private static void chainMine(Level level, BlockPos sourceBlock, int max) {
        BlockPos.breadthFirstTraversal(sourceBlock, Integer.MAX_VALUE, max, (blockPos, blockPosConsumer) -> {
            for (Direction direction : Direction.values()) {
                blockPosConsumer.accept(blockPos.relative(direction));
            }
        }, blockPos -> {
            if (level.getBlockState(blockPos).is(BlockTags.LOGS)) {
                level.destroyBlock(blockPos, true);
                return true;
            }
            return sourceBlock.equals(blockPos);
        });
    }
}
