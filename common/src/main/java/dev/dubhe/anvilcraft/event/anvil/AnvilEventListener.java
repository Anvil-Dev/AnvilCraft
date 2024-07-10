package dev.dubhe.anvilcraft.event.anvil;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilHurtEntityEvent;
import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.mixin.accessor.BaseSpawnerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class AnvilEventListener {

    /**
     * 侦听铁砧落地事件
     *
     * @param event 铁砧落地事件
     */
    @SubscribeEvent
    public void onLand(@NotNull AnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        dropCrabItems(level, pos);
        MinecraftServer server = level.getServer();
        if (null == server) return;
        BlockPos belowPos = pos.below();
        BlockState state = level.getBlockState(belowPos);
        if (state.is(Blocks.REDSTONE_BLOCK)) redstoneEmp(level, belowPos, event.getFallDistance());
        if (state.is(Blocks.SPAWNER)) hitSpawner(level, belowPos, event.getFallDistance());
        if (state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST)) hitBeeNest(level, state, belowPos);
        belowPos = belowPos.below();
        state = level.getBlockState(belowPos);
        if (state.is(Blocks.STONECUTTER)) brokeBlock(level, belowPos.above(), event);
        AnvilCraftingContext context = new AnvilCraftingContext(level, pos, event.getEntity());
        Optional<AnvilRecipe> optional = AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(recipe -> !recipe.getAnvilRecipeType().equals(AnvilRecipeType.MULTIBLOCK_CRAFTING)
                        && recipe.matches(context, level))
                .findFirst();
        optional.ifPresent(anvilRecipe -> anvilProcess(anvilRecipe, context, event));
    }

    private void hitBeeNest(Level level, BlockState state, BlockPos pos) {
        if (!state.hasBlockEntity()) return;
        int honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < BeehiveBlock.MAX_HONEY_LEVELS) return;
        BlockPos potPos = pos.below();
        BlockState pot = level.getBlockState(potPos);
        if (pot.is(Blocks.CAULDRON)) {
            level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 0));
            level.setBlockAndUpdate(
                potPos,
                ModBlocks.HONEY_CAULDRON.getDefaultState()
            );
            level.setBlockAndUpdate(
                potPos,
                level.getBlockState(potPos)
                    .setValue(LayeredCauldronBlock.LEVEL, 1)
            );
        } else {
            if (pot.is(ModBlocks.HONEY_CAULDRON.get())) {
                int cauldronHoneyLevel = pot.getValue(LayeredCauldronBlock.LEVEL);
                level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 0));
                if (cauldronHoneyLevel < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                    level.setBlockAndUpdate(
                        potPos,
                        pot.setValue(LayeredCauldronBlock.LEVEL, cauldronHoneyLevel + 1)
                    );
                } else {
                    level.setBlockAndUpdate(
                        potPos,
                        Blocks.CAULDRON.defaultBlockState()
                    );
                    this.returnItems(level, potPos, List.of(Items.HONEY_BLOCK.getDefaultInstance()));
                }
            }
        }
    }

    private void returnItems(@NotNull Level level, @NotNull BlockPos pos, @NotNull List<ItemStack> items) {
        for (ItemStack item : items) {
            ItemStack type = item.copy();
            type.setCount(1);
            int maxSize = item.getMaxStackSize();
            int count = item.getCount();
            while (count > 0) {
                int size = Math.min(maxSize, count);
                count -= size;
                ItemStack stack = type.copy();
                stack.setCount(size);
                Vec3 vec3 = pos.getCenter();
                ItemEntity entity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, stack, 0.0d, 0.0d, 0.0d);
                level.addFreshEntity(entity);
            }
        }
    }

    private void hitSpawner(Level level, BlockPos pos, float fallDistance) {
        if (level instanceof ServerLevel serverLevel) {
            RandomSource randomSource = serverLevel.getRandom();
            float f = randomSource.nextFloat();
            if (fallDistance < 1) {
                fallDistance = 1.1f;
            }
            if (f <= (1 / fallDistance)) {
                return;
            }
            if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity blockEntity) {

                BaseSpawner spawner = blockEntity.getSpawner();
                BaseSpawnerAccessor accessor = (BaseSpawnerAccessor) spawner;
                SpawnData spawnData = accessor.invoker$getOrCreateNextSpawnData(level, randomSource, pos);
                spawnEntities(spawnData, serverLevel, pos, randomSource, accessor);
            }
        }
    }

    private void spawnEntities(
        SpawnData spawnData,
        ServerLevel serverLevel,
        BlockPos pos,
        RandomSource randomSource,
        @NotNull BaseSpawnerAccessor accessor
    ) {
        for (int i = 0; i < accessor.getSpawnCount(); ++i) {
            CompoundTag compoundTag = spawnData.getEntityToSpawn();
            Optional<EntityType<?>> optional = EntityType.by(compoundTag);
            if (optional.isEmpty()) {
                return;
            }

            ListTag listTag = compoundTag.getList("Pos", 6);
            int size = listTag.size();
            double x;
            double y;
            double z;
            if (size >= 1) {
                x = listTag.getDouble(0);
            } else {
                x = (double) pos.getX()
                    + (randomSource.nextDouble() - randomSource.nextDouble())
                    * accessor.getSpawnRange() + 0.5;
            }
            if (size >= 2) {
                y = listTag.getDouble(1);
            } else {
                y = pos.getY() + randomSource.nextInt(3) - 1;
            }
            if (size >= 3) {
                z = listTag.getDouble(2);
            } else {
                z = (double) pos.getZ()
                    + (randomSource.nextDouble() - randomSource.nextDouble())
                    * accessor.getSpawnRange() + 0.5;
            }
            if (serverLevel.noCollision(optional.get().getAABB(x, y, z))) {
                BlockPos blockPos = BlockPos.containing(x, y, z);
                if (spawnData.getCustomSpawnRules().isPresent()) {
                    if (!optional.get().getCategory().isFriendly()
                        && serverLevel.getDifficulty() == Difficulty.PEACEFUL
                    ) {
                        continue;
                    }

                    SpawnData.CustomSpawnRules customSpawnRules = spawnData.getCustomSpawnRules().get();
                    if (!customSpawnRules.blockLightLimit()
                        .isValueInRange(serverLevel.getBrightness(LightLayer.BLOCK, blockPos))
                        || !customSpawnRules.skyLightLimit()
                        .isValueInRange(serverLevel.getBrightness(LightLayer.SKY, blockPos))) {
                        continue;
                    }
                } else if (!SpawnPlacements.checkSpawnRules(
                    optional.get(),
                    serverLevel,
                    MobSpawnType.SPAWNER,
                    blockPos,
                    serverLevel.getRandom()
                )) {
                    continue;
                }
                Entity entity = EntityType.loadEntityRecursive(compoundTag, serverLevel, it -> {
                    it.moveTo(x, y, z, it.getYRot(), it.getXRot());
                    return it;
                });
                if (entity == null) {
                    return;
                }
                AABB boundingBox = new AABB(
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    pos.getX() + 1,
                    pos.getY() + 1,
                    pos.getZ() + 1
                );
                int k = serverLevel.getEntitiesOfClass(
                    entity.getClass(),
                    boundingBox.inflate(accessor.getSpawnRange())
                ).size();
                if (k >= accessor.getMaxNearbyEntities()) {
                    return;
                }

                entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), randomSource.nextFloat() * 360.0F, 0.0F);
                if (entity instanceof Mob mob) {
                    if (spawnData.getCustomSpawnRules().isEmpty()
                        && !mob.checkSpawnRules(serverLevel, MobSpawnType.SPAWNER)
                        || !mob.checkSpawnObstruction(serverLevel)) {
                        continue;
                    }

                    if (spawnData.getEntityToSpawn().size() == 1 && spawnData.getEntityToSpawn().contains("id", 8)) {
                        ((Mob) entity).finalizeSpawn(
                            serverLevel,
                            serverLevel.getCurrentDifficultyAt(entity.blockPosition()),
                            MobSpawnType.SPAWNER,
                            null,
                            null
                        );
                    }
                }

                if (!serverLevel.tryAddFreshEntityWithPassengers(entity)) {
                    return;
                }

                serverLevel.levelEvent(2004, pos, 0);
                serverLevel.gameEvent(entity, GameEvent.ENTITY_PLACE, blockPos);
                if (entity instanceof Mob) {
                    ((Mob) entity).spawnAnim();
                }
            }
        }
    }

    private void anvilProcess(AnvilRecipe recipe, AnvilCraftingContext context, AnvilFallOnLandEvent event) {
        int counts = 0;
        while (counts < AnvilCraft.config.anvilEfficiency) {
            if (!recipe.craft(context)) break;
            counts++;
        }
        if (context.isAnvilDamage()) event.setAnvilDamage(true);
        context.spawnItemEntity();
    }

    private void brokeBlock(@NotNull Level level, BlockPos pos, AnvilFallOnLandEvent event) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock().getExplosionResistance() >= 1200.0) event.setAnvilDamage(true);
        if (state.getDestroySpeed(level, pos) < 0) return;
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        LootParams.Builder builder = new LootParams
            .Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, false);
        if (state.getBlock() instanceof IHasMultiBlock multiBlock) {
            multiBlock.onRemove(level, pos, state);
        }
        this.dropItems(state.getDrops(builder), level, pos.getCenter());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private void redstoneEmp(@NotNull Level level, @NotNull BlockPos pos, float fallDistance) {
        int radius = AnvilCraft.config.redstoneEmpRadius;
        int maxRadius = AnvilCraft.config.redstoneEmpMaxRadius;
        int distance = Math.min(((int) Math.ceil(fallDistance)) * radius, maxRadius);
        if (!level.getBlockState(pos.relative(Direction.EAST)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = 1; x < distance; x++) {
                for (int z = -distance; z < distance; z++) {
                    redstoneEmp(level, pos.offset(x, 0, z));
                }
            }
        }
        if (!level.getBlockState(pos.relative(Direction.WEST)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = -1; x > -distance; x--) {
                for (int z = -distance; z < distance; z++) {
                    redstoneEmp(level, pos.offset(x, 0, z));
                }
            }
        }
        if (!level.getBlockState(pos.relative(Direction.SOUTH)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = -distance; x < distance; x++) {
                for (int z = 1; z < distance; z++) {
                    redstoneEmp(level, pos.offset(x, 0, z));
                }
            }
        }
        if (!level.getBlockState(pos.relative(Direction.NORTH)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = -distance; x < distance; x++) {
                for (int z = -1; z > -distance; z--) {
                    redstoneEmp(level, pos.offset(x, 0, z));
                }
            }
        }
    }

    private void redstoneEmp(@NotNull Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlockTags.REDSTONE_TORCH)) return;
        state = state.setValue(RedstoneTorchBlock.LIT, false);
        level.setBlockAndUpdate(pos, state);
    }

    private void dropCrabItems(@NotNull Level level, @NotNull BlockPos pos) {
        BlockState state = level.getBlockState(pos.relative(Direction.DOWN));
        if (state.is(ModBlocks.CRAB_TRAP.get())) {
            if (!state.hasBlockEntity()) return;
            CrabTrapBlockEntity blockEntity = (CrabTrapBlockEntity) level.getBlockEntity(pos.relative(Direction.DOWN));
            Direction face = state.getValue(CrabTrapBlock.FACING);
            Vec3 dropPos = pos.relative(face).getCenter().relative(face.getOpposite(), 0.5);
            if (blockEntity == null) return;
            ItemDepository depository = blockEntity.getDepository();
            for (int i = 0; i < depository.getSlots(); i++) {
                ItemStack stack = depository.getStack(i);
                ItemEntity itemEntity = new ItemEntity(level, dropPos.x, dropPos.y - 0.4, dropPos.z, stack, 0, 0, 0);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
                depository.extract(i, stack.getCount(), false);
            }
        }
    }

    /**
     * 侦听铁砧伤害实体事件
     *
     * @param event 铁砧伤害实体事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onAnvilHurtEntity(@NotNull AnvilHurtEntityEvent event) {
        Entity hurtedEntity = event.getHurtedEntity();
        if (!(hurtedEntity instanceof LivingEntity entity)) return;
        if (!(hurtedEntity.level() instanceof ServerLevel serverLevel)) return;
        float damage = event.getDamage();
        float maxHealth = entity.getMaxHealth();
        double rate = damage / maxHealth;
        if (rate < 0.4) return;
        FallingBlockEntity eventEntity = event.getEntity();
        DamageSource source = entity.level().damageSources().anvil(eventEntity);
        LootParams.Builder builder = new LootParams.Builder(serverLevel);
        builder.withParameter(LootContextParams.DAMAGE_SOURCE, source);
        builder.withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, eventEntity);
        builder.withOptionalParameter(LootContextParams.KILLER_ENTITY, eventEntity);
        builder.withParameter(LootContextParams.THIS_ENTITY, entity);
        Vec3 pos = entity.position();
        builder.withParameter(LootContextParams.ORIGIN, pos);
        LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
        LootTable lootTable = serverLevel.getServer().getLootData().getLootTable(entity.getLootTable());
        this.dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
        if (rate >= 0.6) this.dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
        if (rate >= 0.8) this.dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
    }

    private void dropItems(@NotNull List<ItemStack> items, Level level, Vec3 pos) {
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(level, pos.x, pos.y, pos.z, item.copy(), 0.0d, 0.0d, 0.0d);
            level.addFreshEntity(entity);
        }
    }

}
