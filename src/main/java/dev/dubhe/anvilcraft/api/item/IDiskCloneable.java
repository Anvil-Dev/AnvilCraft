package dev.dubhe.anvilcraft.api.item;

import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 可用磁盘复制的方块
 */
public interface IDiskCloneable {

    void storeDiskData(CompoundTag tag);

    void applyDiskData(CompoundTag data);

    /**
     *
     */
    default InteractionResult useDisk(
        Level level,
        Player player,
        InteractionHand hand,
        ItemStack itemStack,
        BlockHitResult hitResult
    ) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (itemStack.is(ModItems.DISK.get())) {
            return itemStack.useOn(new UseOnContext(level, player, hand, itemStack, hitResult));
        }
        return InteractionResult.PASS;
    }
}
