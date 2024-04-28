package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.entity.HasMobBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class HasMobBlockItem extends BlockItem {
    public HasMobBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack, @Nullable Level level,
        @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced
    ) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        Entity entity = HasMobBlockItem.getMobFromItem(level, stack);
        if (entity != null) {
            tooltipComponents.add(
                Component.literal("- ").append(entity.getDisplayName()).withStyle(ChatFormatting.DARK_GRAY)
            );
        }
    }

    public static boolean hasMob(@NotNull ItemStack stack) {
        if (!stack.hasTag()) return false;
        return stack.getOrCreateTag().contains("entity");
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        InteractionResult result = super.useOn(context);
        Level level = context.getLevel();
        Direction clickedFace = context.getClickedFace();
        BlockPos pos = context.getClickedPos();
        ItemStack item = context.getItemInHand();
        BlockEntity blockEntity = level.getBlockEntity(pos.relative(clickedFace));
        if (
            blockEntity != null
                && item.getItem() instanceof HasMobBlockItem
                && blockEntity instanceof HasMobBlockEntity entity
        ) {
            entity.setEntity(HasMobBlockItem.getMobFromItem(level, item));
        }
        return result;
    }

    /**
     * 获取物品中的实体
     */
    public static @Nullable Entity getMobFromItem(Level level, @NotNull ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("entity")) return null;
        CompoundTag entityTag = tag.getCompound("entity");
        Optional<EntityType<?>> optional = EntityType.by(entityTag);
        if (optional.isEmpty()) return null;
        EntityType<?> type = optional.get();
        Entity entity = type.create(level);
        if (entity == null) return null;
        entity.load(entityTag);
        return entity;
    }

    /**
     * 向物品中存入实体
     */
    public static void saveMobInItem(
        @NotNull Level level, @NotNull Mob entity, @NotNull Player player, @NotNull ItemStack stack
    ) {
        stack = stack.split(1);
        if (level.isClientSide()) {
            Item item = stack.getItem();
            if (item instanceof ResinBlockItem item1) {
                BlockPos blockPos = entity.getOnPos();
                BlockState blockState = item1.getBlock().defaultBlockState();
                SoundType soundType = blockState.getSoundType();
                level.playSound(
                    player, blockPos, item1.getPlaceSound(blockState), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f
                );
            }
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (entity instanceof Monster monster) {
            MobEffectInstance instance = monster.getEffect(MobEffects.WEAKNESS);
            if (instance == null) return;
            tag.putBoolean("is_monster", true);
        } else {
            tag.putBoolean("is_monster", false);
        }
        CompoundTag entityTag = new CompoundTag();
        entity.save(entityTag);
        entityTag.remove(Entity.UUID_TAG);
        tag.put("entity", entityTag);
        stack.setTag(tag);
        player.getInventory().placeItemBackInInventory(stack);
        entity.remove(Entity.RemovalReason.DISCARDED);
    }
}
