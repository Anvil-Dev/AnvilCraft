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

public class FellingEnchantment extends Enchantment {
    private static final List<BlockPos> OFFSET = new ArrayList<>() {
        {
            this.add(new BlockPos(-1, -1, -1));
            this.add(new BlockPos(-1, -1, 1));
            this.add(new BlockPos(-1, 1, -1));
            this.add(new BlockPos(-1, 1, 1));
            this.add(new BlockPos(1, -1, -1));
            this.add(new BlockPos(1, -1, 1));
            this.add(new BlockPos(1, 1, -1));
            this.add(new BlockPos(1, 1, 1));
            this.add(new BlockPos(0, 0, -1));
            this.add(new BlockPos(0, 0, 1));
            this.add(new BlockPos(0, -1, 0));
            this.add(new BlockPos(0, 1, 0));
            this.add(new BlockPos(-1, 0, 0));
            this.add(new BlockPos(1, 0, 0));
            this.add(new BlockPos(-1, -1, 0));
            this.add(new BlockPos(-1, 1, 0));
            this.add(new BlockPos(-1, 0, -1));
            this.add(new BlockPos(-1, 0, 1));
            this.add(new BlockPos(0, -1, -1));
            this.add(new BlockPos(0, -1, 1));
            this.add(new BlockPos(0, 1, -1));
            this.add(new BlockPos(0, 1, 1));
            this.add(new BlockPos(1, -1, 0));
            this.add(new BlockPos(1, 1, 0));
            this.add(new BlockPos(1, 0, -1));
            this.add(new BlockPos(1, 0, 1));
        }
    };

    public FellingEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] applicableSlots) {
        super(rarity, category, applicableSlots);
    }

    @Override
    public boolean canEnchant(@NotNull ItemStack stack) {
        return stack.getItem() instanceof AxeItem;
    }

    public int getMinCost(int level) {
        return 1 + (level - 1) * 10;
    }

    public int getMaxCost(int level) {
        return this.getMinCost(level) + 15;
    }

    public int getMaxLevel() {

        return 3;
    }

    /**
     * 伐木
     *
     * @param level 世界
     * @param pos   位置
     */
    public static void felling(Player player, Level level, BlockPos pos, ItemStack tool) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(tool);
        if (!enchantments.containsKey(ModEnchantments.FELLING.get())) return;
        int max = enchantments.get(ModEnchantments.FELLING.get()) * AnvilCraft.config.fellingBlockPerLevel;
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
                    if (!state.is(BlockTags.LOGS)) continue;
                    if (max-- <= 0) break felling;
                    posList2.add(nextOffset);
                    Block block = state.getBlock();
                    block.playerWillDestroy(level, nextOffset, state, player);
                    boolean bl = level.removeBlock(nextOffset, false);
                    if (bl) {
                        block.destroy(level, nextOffset, state);
                    }
                    if (player.isCreative()) continue;
                    BlockEntity entity = level.getBlockEntity(nextOffset);
                    block.playerDestroy(level, player, nextOffset, state, entity, tool);
                }
                iterator.remove();
            }
            posList.addAll(posList2);
        }
    }
}
