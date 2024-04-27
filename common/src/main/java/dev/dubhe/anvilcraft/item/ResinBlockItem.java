package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
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
        if (level.isClientSide()) {
            Item item = stack.getItem();
            if (item instanceof ResinBlockItem item1) {
                BlockState blockState = item1.getBlock().defaultBlockState();
                SoundType soundType = blockState.getSoundType();
                level.playSound(
                    context.getPlayer(), pos, item1.getPlaceSound(blockState), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f
                );
            }
            return InteractionResult.SUCCESS;
        }
        ResinBlockItem.spawnMobFromItem(level, pos, stack);
        return InteractionResult.SUCCESS;
    }

    /**
     * 右键实体
     */
    public static InteractionResult useEntity(Player player, @NotNull Entity target, ItemStack stack) {
        if (!(target instanceof Mob mob)
            || target.getBbHeight() > 2.0
            || target.getBbWidth() > 1.5
            || ResinBlockItem.hasMob(stack)
        ) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            Item item = stack.getItem();
            if (item instanceof ResinBlockItem item1) {
                BlockPos blockPos = target.getOnPos();
                BlockState blockState = item1.getBlock().defaultBlockState();
                SoundType soundType = blockState.getSoundType();
                level.playSound(
                    player, blockPos, item1.getPlaceSound(blockState), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f
                );
            }
            return InteractionResult.SUCCESS;
        }
        ResinBlockItem.saveMobInItem(mob, player, stack);
        return InteractionResult.SUCCESS;
    }

    private static void spawnMobFromItem(Level level, BlockPos blockPos, @NotNull ItemStack stack) {
        Entity entity = HasMobBlockItem.getMobFromItem(level, stack);
        if (entity == null) return;
        stack.resetHoverName();
        CompoundTag tag = stack.getOrCreateTag();
        tag.remove("entity");
        stack.setTag(tag);
        entity.moveTo(blockPos.getCenter());
        level.addFreshEntity(entity);
    }

    private static void saveMobInItem(@NotNull Mob entity, Player player, @NotNull ItemStack stack) {
        stack = stack.split(1);
        CompoundTag entityTag = new CompoundTag();
        entity.save(entityTag);
        entityTag.remove(Entity.UUID_TAG);
        MutableComponent name = Component.translatable(
            "block.anvilcraft.resin_block.has_mob",
            entity.getDisplayName().copy().withStyle(ChatFormatting.DARK_PURPLE)
        ).withStyle(ChatFormatting.AQUA).withStyle(Style.EMPTY.withItalic(false));
        stack.setHoverName(name);
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("entity", entityTag);
        stack.setTag(tag);
        player.getInventory().placeItemBackInInventory(stack);
        entity.remove(Entity.RemovalReason.DISCARDED);
    }
}
