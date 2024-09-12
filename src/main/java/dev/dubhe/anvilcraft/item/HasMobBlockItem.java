package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HasMobBlockItem extends BlockItem {
    public HasMobBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
        if (!HasMobBlockItem.hasMob(stack)) return;
        Entity entity = HasMobBlockItem.getMobFromItem(context.level(), stack);
        if (entity != null) {
            tooltipComponents.add(Component.literal("- ")
                    .append(entity.getDisplayName())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static boolean hasMob(@NotNull ItemStack stack) {
        return stack.has(ModComponents.SAVED_ENTITY);
    }

    /**
     * 获取物品中的实体
     */
    public static @Nullable Entity getMobFromItem(Level level, @NotNull ItemStack stack) {
        if (!hasMob(stack)) return null;
        SavedEntity savedEntity = stack.get(ModComponents.SAVED_ENTITY);
        // make idea happy
        if (savedEntity == null) return null;
        return savedEntity.toEntity(level);
    }

    /**
     * 向物品中存入实体
     */
    public static void saveMobInItem(
            @NotNull Level level, @NotNull Mob entity, @NotNull Player player, @NotNull ItemStack stack) {
        stack = stack.split(1);
        if (level.isClientSide()) {
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
                        soundType.getPitch() * 0.8f);
            }
            return;
        }
        SavedEntity savedEntity = SavedEntity.fromMob(entity);
        if (entity instanceof Monster monster) {
            MobEffectInstance instance = monster.getEffect(MobEffects.WEAKNESS);
            if (instance == null && !player.getAbilities().instabuild) return;
        }
        stack.set(ModComponents.SAVED_ENTITY, savedEntity);
        player.getInventory().placeItemBackInInventory(stack);
        if (entity instanceof Villager villager) {
            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);
        }
        entity.remove(Entity.RemovalReason.DISCARDED);
    }

    public static class SavedEntity {
        public static final Codec<SavedEntity> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                        CompoundTag.CODEC.fieldOf("tag").forGetter(o -> o.tag),
                        Codec.BOOL.fieldOf("isMonster").forGetter(o -> o.isMonster))
                .apply(ins, SavedEntity::new));

        public static final StreamCodec<ByteBuf, SavedEntity> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG,
                o -> o.tag,
                ByteBufCodecs.BOOL,
                o -> o.isMonster,
                SavedEntity::new);

        private final CompoundTag tag;
        private final boolean isMonster;

        public SavedEntity(CompoundTag tag, boolean isMonster) {
            this.tag = tag;
            this.isMonster = isMonster;
        }

        public Entity toEntity(Level level) {
            Optional<EntityType<?>> optional = EntityType.by(tag);
            if (optional.isEmpty()) return null;
            EntityType<?> type = optional.get();
            Entity entity = type.create(level);
            if (entity == null) return null;
            entity.load(tag);
            return entity;
        }

        public static SavedEntity fromMob(Mob entity) {
            CompoundTag entityTag = new CompoundTag();
            entity.saveAsPassenger(entityTag);
            entityTag.remove(Entity.UUID_TAG);
            return new SavedEntity(entityTag, entity instanceof Monster);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof SavedEntity savedEntity) {
                return tag.equals(savedEntity.tag);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(tag);
        }
    }
}
