package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

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
        if (!HasMobBlockItem.hasMob(stack)) return;
        Entity entity = HasMobBlockItem.getMobFromItem(level, stack);
        if (entity != null) {
            tooltipComponents.add(
                    Component.literal("- ").append(entity.getDisplayName()).withStyle(ChatFormatting.DARK_GRAY)
            );
        }
    }

    public static boolean hasMob(@NotNull ItemStack stack) {
        if (!stack.hasTag() || !stack.getOrCreateTag().contains("BlockEntityTag")) return false;
        return stack.getOrCreateTag().getCompound("BlockEntityTag").contains("entity");
    }

    /**
     * 获取物品中的实体
     */
    public static @Nullable Entity getMobFromItem(Level level, @NotNull ItemStack stack) {
        if (!stack.hasTag()) return null;
        CompoundTag tag = stack.getOrCreateTag();
        tag = tag.contains("BlockEntityTag") ? tag.getCompound("BlockEntityTag") : new CompoundTag();
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
        CompoundTag entityTag = new CompoundTag();
        entity.saveAsPassenger(entityTag);
        entityTag.remove(Entity.UUID_TAG);
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("entity", entityTag);
        CompoundTag compoundTag = new CompoundTag();
        if (entity instanceof Monster monster) {
            MobEffectInstance instance = monster.getEffect(MobEffects.WEAKNESS);
            if (instance == null && !player.getAbilities().instabuild) return;
            compoundTag.putBoolean("is_monster", true);
        } else {
            compoundTag.putBoolean("is_monster", false);
        }
        compoundTag.put("BlockEntityTag", tag);
        stack.setTag(compoundTag);
        player.getInventory().placeItemBackInInventory(stack);
        if (entity instanceof Villager villager) {
            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);
        }
        entity.remove(Entity.RemovalReason.DISCARDED);
    }
}
