package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Singularity crystal item that stores extreme celestial body snapshots.
 * Shows enchantment glint when data is stored.
 */
public class SingularityCrystalItem extends BlockItem {

    public SingularityCrystalItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return CelestialForgingAnvilBlockEntity.loadSnapshotFromStack(stack) != null;
    }
}
