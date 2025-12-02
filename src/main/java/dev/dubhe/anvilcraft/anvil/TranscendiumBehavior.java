package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

// TODO: 临时硬编码解决方案
public class TranscendiumBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        final RandomSource random = level.getRandom();
        final List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, new AABB(hitBlockPos.above()));

        if (!hitBlockState.is(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK)) {
            return false;
        }

        if (itemEntities.isEmpty()) {
            return false;
        }

        ItemEntity chargedNeutroniumIngotItemEntity = itemEntities.getFirst();
        ItemStack chargedNeutroniumIngotItem = chargedNeutroniumIngotItemEntity.getItem();

        if (!chargedNeutroniumIngotItem.is(ModItems.CHARGED_NEUTRONIUM_INGOT) || chargedNeutroniumIngotItem.getCount() != 1) {
            return false;
        }

        ItemEnchantments enchantments = chargedNeutroniumIngotItem.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int enchantmentCount = enchantments.size();

        level.setBlockAndUpdate(hitBlockPos, Blocks.AIR.defaultBlockState());
        this.discardItemEntity(chargedNeutroniumIngotItemEntity);

        if (enchantmentCount == 0) {
            this.spawnItemEntity(level, hitBlockPos, ModItems.TRANSCENDIUM_INGOT.asStack(4));
        } else if (enchantmentCount >= 1 && enchantmentCount <= 10) {
            if (random.nextDouble() < 10 * enchantmentCount / 100f) {
                this.spawnItemEntity(level, hitBlockPos, ModItems.NEUTRONIUM_INGOT.asStack());
            }
            this.spawnItemEntity(level, hitBlockPos, ModItems.TRANSCENDIUM_INGOT.asStack(4));
            this.spawnItemEntity(level, hitBlockPos, ModItems.TRANSCENDIUM_NUGGET.asStack(3 * enchantmentCount));
        } else if (enchantmentCount >= 11 && enchantmentCount <= 14) {
            this.spawnItemEntity(level, hitBlockPos, ModItems.NEUTRONIUM_INGOT.asStack());
            this.spawnItemEntity(level, hitBlockPos, ModItems.TRANSCENDIUM_INGOT.asStack(4));
            this.spawnItemEntity(level, hitBlockPos, ModItems.TRANSCENDIUM_NUGGET.asStack(3 * enchantmentCount));
        } else if (enchantmentCount == 15) {
            this.spawnItemEntity(level, hitBlockPos, ModItems.NEUTRONIUM_INGOT.asStack());
            level.setBlockAndUpdate(hitBlockPos, ModBlocks.TRANSCENDIUM_BLOCK.getDefaultState());
        } else if (enchantmentCount >= 16) {
            this.spawnItemEntity(level, hitBlockPos, ModItems.NEUTRONIUM_INGOT.asStack());
            this.spawnItemEntity(level, hitBlockPos, ModItems.TRANSCENDIUM_NUGGET.asStack(enchantmentCount));
            level.setBlockAndUpdate(hitBlockPos, ModBlocks.TRANSCENDIUM_BLOCK.getDefaultState());
        }
        return true;
    }

    private void discardItemEntity(ItemEntity itemEntity) {
        if (itemEntity.isAlive()) {
            itemEntity.discard();
        }
    }

    private void spawnItemEntity(Level level, BlockPos pos, ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
            level.addFreshEntity(itemEntity);
        }
    }
}
