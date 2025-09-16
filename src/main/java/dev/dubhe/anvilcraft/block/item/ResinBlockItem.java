package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
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

public class ResinBlockItem extends HasMobBlockItem {
    public ResinBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!ResinBlockItem.hasMob(stack)) return super.useOn(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        if (player != null) {
            ResinBlockItem.spawnMobFromItem(level, player, pos, stack);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 右键实体
     */
    public static InteractionResult useEntity(Player player, Entity target, ItemStack stack) {
        if (!(target instanceof Mob mob)
            || target.getBbHeight() > 2.0
            || target.getBbWidth() > 1.5
            || ResinBlockItem.hasMob(stack)) {
            return InteractionResult.PASS;
        }
        ResinBlockItem.saveMobInItem(player.level(), mob, player, stack);
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    private static void spawnMobFromItem(Level level, Player player, BlockPos pos, ItemStack stack) {
        ItemStack copy = stack.copy();
        stack.shrink(1);
        stack.remove(ModComponents.SAVED_ENTITY);
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
        ItemStack back = new ItemStack(ModItems.RESIN.asItem(), random.nextInt(1, 4));
        if (!player.getAbilities().instabuild) {
            player.getInventory().placeItemBackInInventory(back);
        }
    }

    @SuppressWarnings("deprecation")
    public static ItemStack spawnMobFromItem(Level level, BlockPos pos, ItemStack stack) {
        stack = stack.split(1);
        if (level.isClientSide()) {
            Item item = stack.getItem();
            if (item instanceof ResinBlockItem item1) {
                BlockState blockState = item1.getBlock().defaultBlockState();
                SoundType soundType = blockState.getSoundType();
                level.playSound(
                    null,
                    pos,
                    item1.getPlaceSound(blockState),
                    SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0f) / 2.0f,
                    soundType.getPitch() * 0.8f);
            }
            return ItemStack.EMPTY;
        }
        Entity entity = HasMobBlockItem.getMobFromItem(level, stack);
        if (entity == null) return stack;
        entity.moveTo(pos.getCenter());
        level.addFreshEntity(entity);
        RandomSource random = level.getRandom();
        return new ItemStack(ModItems.RESIN.asItem(), random.nextInt(1, 4));
    }
}
