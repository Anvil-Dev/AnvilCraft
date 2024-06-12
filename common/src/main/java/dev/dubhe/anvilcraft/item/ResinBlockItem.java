package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

public class ResinBlockItem extends HasMobBlockItem {
    public ResinBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!ResinBlockItem.hasMob(stack)) return super.useOn(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        ResinBlockItem.spawnMobFromItem(level, player, pos, stack);
        return InteractionResult.SUCCESS;
    }

    /**
     * 右键实体
     */
    public static InteractionResult useEntity(
            Player player, @NotNull Entity target, ItemStack stack) {
        if (!(target instanceof Mob mob)
                || target.getBbHeight() > 2.0
                || target.getBbWidth() > 1.5
                || ResinBlockItem.hasMob(stack)) {
            return InteractionResult.PASS;
        }
        ResinBlockItem.saveMobInItem(player.level(), mob, player, stack);
        return InteractionResult.SUCCESS;
    }

    private static void spawnMobFromItem(
            @NotNull Level level, Player player, BlockPos pos, @NotNull ItemStack stack) {
        ItemStack copy = stack.copy();
        stack.shrink(1);
        if (level.isClientSide()) {
            Item item = copy.getItem();
            if (item instanceof ResinBlockItem item1) {
                BlockState blockState = item1.getBlock().defaultBlockState();
                SoundType soundType = blockState.getSoundType();
                level.playSound(
                        player,
                        pos,
                        item1.getPlaceSound(blockState),
                        SoundSource.BLOCKS,
                        (soundType.getVolume() + 1.0f) / 2.0f,
                        soundType.getPitch() * 0.8f);
            }
            return;
        }
        Entity entity = HasMobBlockItem.getMobFromItem(level, copy);
        if (entity == null) return;
        entity.moveTo(pos.getCenter());
        level.addFreshEntity(entity);
        RandomSource random = level.getRandom();
        ItemStack back = new ItemStack(ModItems.RESIN, random.nextInt(1, 4));
        if (!player.getAbilities().instabuild) player.getInventory().placeItemBackInInventory(back);
    }
}
