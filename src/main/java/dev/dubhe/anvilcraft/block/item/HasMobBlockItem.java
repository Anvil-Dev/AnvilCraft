package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HasMobBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class HasMobBlockItem extends BlockItem {
    public HasMobBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {
        super.verifyComponentsAfterLoad(stack);
        if (stack.has(ModComponents.SAVED_ENTITY)) return;
        if (
            !stack.is(ModBlocks.MOB_AMBER_BLOCK.asItem())
            && !stack.is(ModBlocks.RESENTFUL_AMBER_BLOCK.asItem())
        ) {
            return;
        }
        ResourceLocation id;
        boolean isMonster = false;
        if (stack.is(ModBlocks.RESENTFUL_AMBER_BLOCK.asItem())) {
            isMonster = true;
            id = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE);
        } else {
            id = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.MOOSHROOM);
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        SavedEntity savedEntity = new SavedEntity(tag, isMonster);
        stack.set(ModComponents.SAVED_ENTITY, savedEntity);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext context,
        List<Component> tooltipComponents,
        TooltipFlag isAdvanced
    ) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
        if (!HasMobBlockItem.hasMob(stack)) return;
        Optional.ofNullable(context.level())
            .map(level -> HasMobBlockItem.getMobFromItem(level, stack))
            .ifPresent(entity -> tooltipComponents.add(
                Component.literal("- ").append(entity.getDisplayName()).withStyle(ChatFormatting.DARK_GRAY)));
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        if (hasMob(stack)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HasMobBlockEntity hmbe) {
                hmbe.setEntity(getMobFromItem(level, stack));
            }
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    public static boolean hasMob(ItemStack stack) {
        return stack.has(ModComponents.SAVED_ENTITY);
    }

    /**
     * 获取物品中的实体
     */
    public static @Nullable Entity getMobFromItem(Level level, ItemStack stack) {
        if (!hasMob(stack)) return null;
        SavedEntity savedEntity = stack.get(ModComponents.SAVED_ENTITY);
        // make idea happy
        if (savedEntity == null) return null;
        return savedEntity.toEntity(level);
    }

    @SuppressWarnings("deprecation")
    public static ItemStack saveMobInItem(Level level, Mob entity, @Nullable Player player, ItemStack stack) {
        if (level.isClientSide()) {
            if (player == null) AnvilCraft.LOGGER.warn("why a dispenser run saveMobInItem in client side???");
            Item item = stack.getItem();
            if (item instanceof ResinBlockItem item1) {
                BlockPos blockPos = entity.getOnPos();
                BlockState blockState = item1.getBlock().defaultBlockState();
                SoundType soundType = blockState.getSoundType();
                level.playSound(
                    player,
                    blockPos,
                    item1.getPlaceSound(blockState),
                    SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0f) / 2.0f,
                    soundType.getPitch() * 0.8f
                );
            }
            return ItemStack.EMPTY;
        }

        SavedEntity savedEntity = SavedEntity.fromMob(entity);
        ItemStack newStack = stack.split(1);
        newStack.set(ModComponents.SAVED_ENTITY, savedEntity);
        if (entity instanceof Villager villager) {
            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);
        }
        entity.remove(Entity.RemovalReason.DISCARDED);
        if (player != null) player.getInventory().placeItemBackInInventory(newStack);
        return newStack;
    }

    public static ItemStack saveMobInItem(Level level, Mob entity, ItemStack stack) {
        return saveMobInItem(level, entity, null, stack);
    }

    public static boolean canMobBeSaved(Mob entity, @Nullable Player player, @Nullable ItemStack stack) {
        if (player != null && player.getAbilities().instabuild) return true;

        if (stack != null && ResinBlockItem.hasMob(stack)) return false;
        if (entity.getBbHeight() > 2.0 || entity.getBbWidth() > 1.5) return false;
        return !(entity instanceof Monster monster && !monster.hasEffect(MobEffects.WEAKNESS));
    }

    public static boolean canMobBeSaved(Mob entity) {
        return canMobBeSaved(entity, null, null);
    }
}
