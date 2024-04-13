package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilHurtEntityEvent;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
        MinecraftServer server = level.getServer();
        if (null == server) return;
        BlockPos belowPos = pos.below();
        BlockState state = level.getBlockState(belowPos);
        if (state.is(Blocks.REDSTONE_BLOCK)) redstoneEmp(level, belowPos, event.getFallDistance());
        belowPos = belowPos.below();
        state = level.getBlockState(belowPos);
        if (state.is(Blocks.STONECUTTER)) brokeBlock(level, belowPos.above(), event);
        AnvilCraftingContainer container = new AnvilCraftingContainer(level, pos, event.getEntity());
        Optional<AnvilRecipe> optional = server
            .getRecipeManager()
            .getRecipeFor(ModRecipeTypes.ANVIL_RECIPE, container, level);
        optional.ifPresent(anvilRecipe -> anvilProcess(anvilRecipe, container, event));
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
}
