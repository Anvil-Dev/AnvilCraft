package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.mixin.accessor.BaseSpawnerAccessor;
import dev.dubhe.anvilcraft.util.ResentmentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ResinBlockItem extends HasMobBlockItem {
    public ResinBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!ResinBlockItem.hasMob(stack)
            && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SpawnerBlockEntity spawner
        ) {
            return captureSpawner(context, spawner);
        }
        if (!ResinBlockItem.hasMob(stack)) return super.useOn(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        if (player != null) {
            ResinBlockItem.spawnMobFromItem(level, player, pos, stack);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult captureSpawner(UseOnContext context, SpawnerBlockEntity blockEntity) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        BaseSpawnerAccessor accessor = (BaseSpawnerAccessor) blockEntity.getSpawner();
        SpawnData spawnData = accessor.invokeGetOrCreateNextSpawnData(level, level.getRandom(), pos);
        CompoundTag entityTag = spawnData.getEntityToSpawn().copy();
        Entity entity = EntityType.loadEntityRecursive(
            entityTag,
            level,
            EntitySpawnReason.SPAWNER,
            EntityProcessor.NOP
        );
        if (!(entity instanceof Mob mob)) return InteractionResult.FAIL;

        ResentmentUtil.setForcedResentment(mob, 100);
        ResinBlockItem.saveMobInItem(level, mob, player, context.getItemInHand());
        accessor.invokeSetNextSpawnData(level, pos, new SpawnData());
        accessor.setSpawnPotentials(WeightedList.of());
        accessor.setDisplayEntity(null);
        blockEntity.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    /// 右键实体
    public static InteractionResult useEntity(Player player, Entity target, ItemStack stack) {
        if (!(target instanceof Mob mob && HasMobBlockItem.canMobBeSaved(mob, player, stack))) {
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
                    (soundType.getVolume() + 1.0F) / 2.0F,
                    soundType.getPitch() * 0.8F);
            }
            return;
        }
        Entity entity = HasMobBlockItem.getMobFromItem(level, copy);
        if (entity == null) return;
        if (copy.has(DataComponents.CUSTOM_NAME)) {
            Component component = copy.get(DataComponents.CUSTOM_NAME);
            entity.setCustomName(component);
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
        }
        entity.moveOrInterpolateTo(pos.getCenter());
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
                    (soundType.getVolume() + 1.0F) / 2.0F,
                    soundType.getPitch() * 0.8F);
            }
            return ItemStack.EMPTY;
        }
        Entity entity = HasMobBlockItem.getMobFromItem(level, stack);
        if (entity == null) return stack;
        entity.moveOrInterpolateTo(pos.getCenter());
        level.addFreshEntity(entity);
        RandomSource random = level.getRandom();
        return new ItemStack(ModItems.RESIN.asItem(), random.nextInt(1, 4));
    }
}
