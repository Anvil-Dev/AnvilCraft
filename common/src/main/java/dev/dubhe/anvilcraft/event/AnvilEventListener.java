package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilHurtEntityEvent;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.inventory.SimpleCraftingContainer;
import dev.dubhe.anvilcraft.mixin.accessor.BaseSpawnerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AnvilEventListener {
    private static final RegistryAccess ACCESS = new RegistryAccess.ImmutableRegistryAccess(List.of());
    private static final List<Item> PASS = List.of(
        Items.IRON_TRAPDOOR,
        Items.PRISMARINE
    );

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
        belowPos = belowPos.below();
        state = level.getBlockState(belowPos);
        if (state.is(Blocks.STONECUTTER)) brokeBlock(level, belowPos.above(), event);
        if (this.isHeating(level, pos) && this.craft(level, pos, this::heating, -1)) return;
        if (this.isSmoking(level, pos) && this.craft(level, pos, this::smoking, -1)) return;
        if (this.isCompress(level, pos) && this.craft(level, pos, this::compress, -1)) return;
        if (this.isSmash(level, pos) && this.craft(level, pos, this::smash, 0)) return;
        AnvilCraftingContainer container = new AnvilCraftingContainer(level, pos, event.getEntity());
        Optional<AnvilRecipe> optional = server
            .getRecipeManager()
            .getRecipeFor(ModRecipeTypes.ANVIL_RECIPE, container, level);
        optional.ifPresent(anvilRecipe -> anvilProcess(anvilRecipe, container, event));
    }

    private List<ItemStack> getItemStackForSpace(@NotNull Level level, @NotNull BlockPos pos) {
        Map<ItemStack, Integer> items = new HashMap<>();
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();
            Optional<ItemStack> optional = items.keySet()
                .stream()
                .filter(i -> ItemStack.isSameItem(i, item))
                .findFirst();
            ItemStack type;
            type = optional.orElseGet(item::copy);
            type.setCount(1);
            items.put(type, items.getOrDefault(type, 0) + item.getCount());
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
        return items.entrySet().stream().map(entry -> {
            ItemStack stack = entry.getKey().copy();
            stack.setCount(entry.getValue());
            return stack;
        }).toList();
    }

    private void returnItems(@NotNull Level level, @NotNull BlockPos pos, @NotNull List<ItemStack> items) {
        for (ItemStack item : items) {
            ItemStack type = item.copy();
            type.setCount(1);
            int maxSize = item.getMaxStackSize();
            for (
                int count = item.getCount(), size = Math.min(maxSize, count);
                count > 0;
                count -= size
            ) {
                ItemStack stack = type.copy();
                stack.setCount(size);
                Vec3 vec3 = pos.getCenter();
                ItemEntity entity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, stack, 0.0d, 0.0d, 0.0d);
                level.addFreshEntity(entity);
            }
        }
    }

    private boolean craft(@NotNull Level level, @NotNull BlockPos pos, Crafting crafting, int inY) {
        MinecraftServer server = level.getServer();
        if (server == null) return false;
        BlockPos in = pos.offset(0, inY, 0);
        List<ItemStack> stacks = this.getItemStackForSpace(level, in);
        List<ItemStack> remainders = new ArrayList<>();
        List<ItemStack> results = new ArrayList<>();
        int count = AnvilCraft.config.anvilEfficiency;
        crafting.crafting(level, stacks, remainders, results, count);
        this.returnItems(level, in, remainders);
        this.returnItems(level, pos.below(), results);
        return !results.isEmpty();
    }

    private boolean isHeating(@NotNull Level level, @NotNull BlockPos pos) {
        BlockState state = level.getBlockState(pos.below());
        BlockState state1 = level.getBlockState(pos.below(2));
        return state.is(Blocks.CAULDRON)
            && state1.is(ModBlocks.HEATER.get())
            && !state1.getValue(IPowerComponent.OVERLOAD);
    }

    private boolean isSmoking(@NotNull Level level, @NotNull BlockPos pos) {
        BlockState state = level.getBlockState(pos.below());
        BlockState state1 = level.getBlockState(pos.below(2));
        return state.is(Blocks.CAULDRON)
            && state1.is(BlockTags.CAMPFIRES)
            && state1.getValue(CampfireBlock.LIT);
    }

    private boolean isCompress(@NotNull Level level, @NotNull BlockPos pos) {
        BlockState state = level.getBlockState(pos.below());
        return state.is(Blocks.CAULDRON);
    }

    private boolean isSmash(@NotNull Level level, @NotNull BlockPos pos) {
        BlockState state = level.getBlockState(pos.below());
        return state.is(Blocks.IRON_TRAPDOOR)
            && !state.getValue(TrapDoorBlock.OPEN)
            && state.getValue(TrapDoorBlock.HALF) == Half.TOP;
    }

    private void compress(
        @NotNull Level level,
        @NotNull List<ItemStack> stacks,
        List<ItemStack> remainders,
        List<ItemStack> results,
        int count
    ) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        for (ItemStack stack : stacks) {
            ItemStack type = stack.copy();
            type.setCount(1);
            int deplete = 9;
            CraftingContainer container = SimpleCraftingContainer.create9x(type);
            Optional<? extends CraftingRecipe> optional = server.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, container, level);
            if (optional.isEmpty()) {
                deplete = 4;
                container = SimpleCraftingContainer.create4x(type);
                optional = server.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, container, level);
            }
            if (optional.isEmpty() || stack.getCount() < deplete) {
                remainders.add(stack);
                continue;
            }
            CraftingRecipe recipe = optional.get();
            ItemStack result = recipe.getResultItem(ACCESS).copy();
            if (PASS.stream().anyMatch(result::is)) {
                remainders.add(stack);
                continue;
            }
            int canCraft = stack.getCount() / deplete;
            int size = Math.min(count, canCraft);
            count -= size;
            int resultCount = result.getCount();
            if (resultCount != 1) {
                remainders.add(stack);
                continue;
            }
            int remainder = stack.getCount() - size * deplete;
            stack.setCount(remainder);
            if (remainder != 0) remainders.add(stack);
            resultCount *= size;
            result.setCount(resultCount);
            results.add(result);
            results.addAll(recipe.getRemainingItems(container)
                .stream()
                .map(ItemStack::copy)
                .peek(item -> item.setCount(item.getCount() * size))
                .toList()
            );
            if (count <= 0) break;
        }
    }

    private void smash(
        @NotNull Level level,
        @NotNull List<ItemStack> stacks,
        List<ItemStack> remainders,
        List<ItemStack> results,
        int count
    ) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        for (ItemStack stack : stacks) {
            ItemStack type = stack.copy();
            type.setCount(1);
            CraftingContainer container = new SimpleCraftingContainer(type);
            Optional<? extends CraftingRecipe> optional = server.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, container, level);
            if (optional.isEmpty()) {
                remainders.add(stack);
                continue;
            }
            int size = Math.min(count, stack.getCount());
            count -= size;
            CraftingRecipe recipe = optional.get();
            ItemStack result = recipe.getResultItem(ACCESS).copy();
            int resultCount = result.getCount();
            if (resultCount == 1) {
                remainders.add(stack);
                continue;
            }
            int remainder = stack.getCount() - size;
            stack.setCount(remainder);
            if (remainder != 0) remainders.add(stack);
            resultCount *= size;
            result.setCount(resultCount);
            results.add(result);
            results.addAll(recipe.getRemainingItems(container)
                .stream()
                .map(ItemStack::copy)
                .peek(item -> item.setCount(item.getCount() * size))
                .toList()
            );
            if (count <= 0) break;
        }
    }

    private void heating(
        @NotNull Level level,
        @NotNull List<ItemStack> stacks,
        List<ItemStack> remainders,
        List<ItemStack> results,
        int count
    ) {
        this.cooking(level, stacks, remainders, results, RecipeType.BLASTING, count, 2);
    }

    private void smoking(
        @NotNull Level level,
        @NotNull List<ItemStack> stacks,
        List<ItemStack> remainders,
        List<ItemStack> results,
        int count
    ) {
        this.cooking(level, stacks, remainders, results, RecipeType.SMOKING, count, 1);
    }

    private void cooking(
        @NotNull Level level,
        @NotNull List<ItemStack> stacks,
        List<ItemStack> remainders,
        List<ItemStack> results,
        RecipeType<? extends AbstractCookingRecipe> recipeType,
        int count,
        int yield
    ) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        for (ItemStack stack : stacks) {
            ItemStack type = stack.copy();
            type.setCount(1);
            SimpleContainer container = new SimpleContainer(type);
            Optional<? extends AbstractCookingRecipe> optional = server.getRecipeManager()
                .getRecipeFor(recipeType, container, level);
            if (optional.isEmpty()) {
                remainders.add(stack);
                continue;
            }
            int size = Math.min(count, stack.getCount());
            count -= size;
            int remainder = stack.getCount() - size;
            stack.setCount(remainder);
            if (remainder != 0) remainders.add(stack);
            AbstractCookingRecipe recipe = optional.get();
            ItemStack result = recipe.getResultItem(ACCESS).copy();
            int resultCount = result.getCount() * size * yield;
            result.setCount(resultCount);
            results.add(result);
            if (count <= 0) break;
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

    private void anvilProcess(AnvilRecipe recipe, AnvilCraftingContainer container, AnvilFallOnLandEvent event) {
        int counts = 0;
        while (counts < AnvilCraft.config.anvilEfficiency) {
            if (!recipe.craft(container)) break;
            counts++;
        }
        if (container.isAnvilDamage()) event.setAnvilDamage(true);
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
        dropItems(state.getDrops(builder), level, pos.getCenter());
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
        dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
        if (rate >= 0.6) dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
        if (rate >= 0.8) dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
    }

    private void dropItems(@NotNull List<ItemStack> items, Level level, Vec3 pos) {
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(level, pos.x, pos.y, pos.z, item.copy());
            level.addFreshEntity(entity);
        }
    }

    @FunctionalInterface
    interface Crafting {
        void crafting(
            @NotNull Level level,
            List<ItemStack> stacks,
            List<ItemStack> remainders,
            List<ItemStack> results,
            int count
        );
    }
}
