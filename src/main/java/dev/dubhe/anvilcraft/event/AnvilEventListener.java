package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilHurtEntityEvent;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.block.BlockAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
    @SubscribeEvent
    public void onLand(@NotNull AnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        AnvilCraftingContainer container = new AnvilCraftingContainer(level, pos, event.getEntity());
        MinecraftServer server = level.getServer();
        if (null == server) return;
        Optional<ItemAnvilRecipe> optional = server.getRecipeManager().getRecipeFor(ModRecipeTypes.ANVIL_ITEM, container, level);
        if (optional.isEmpty()) {
            Optional<BlockAnvilRecipe> optional1 = server.getRecipeManager().getRecipeFor(ModRecipeTypes.ANVIL_BLOCK, container, level);
            optional1.ifPresent(blockAnvilRecipe -> blockProcess(blockAnvilRecipe, container, level, event));
        } else itemProcess(optional.get(), container, level, event);
        BlockPos belowPos = pos.below();
        BlockState state = level.getBlockState(belowPos);
        if (state.is(Blocks.REDSTONE_BLOCK)) redstoneEMP(level, belowPos);
        belowPos = belowPos.below();
        state = level.getBlockState(belowPos);
        if (state.is(Blocks.STONECUTTER)) brokeBlock(level, belowPos.above(), event);
    }

    private void itemProcess(ItemAnvilRecipe recipe, AnvilCraftingContainer container, Level level, AnvilFallOnLandEvent event) {
        int counts = 0;
        while (counts < AnvilCraft.config.anvilEfficiency) {
            if (!recipe.craft(container, level)) break;
            counts++;
        }
        BlockPos resultPos = new BlockPos(container.pos());
        if (recipe.getResultLocation() == ItemAnvilRecipe.Location.IN) resultPos = resultPos.below();
        if (recipe.getResultLocation() == ItemAnvilRecipe.Location.UNDER) resultPos = resultPos.below(2);
        for (ItemStack itemStack : recipe.getResults()) {
            int maxSize = itemStack.getItem().getMaxStackSize();
            counts = counts * itemStack.getCount();
            Vec3 vec3 = resultPos.getCenter();
            for (int i = 0; i < counts / maxSize; i++) {
                ItemStack stack = itemStack.copy();
                stack.setCount(maxSize);
                ItemEntity entity = new ItemEntity(EntityType.ITEM, level);
                entity.setItem(stack);
                entity.teleportRelative(vec3.x, vec3.y, vec3.z);
                level.addFreshEntity(entity);
            }
            ItemStack stack = itemStack.copy();
            stack.setCount(counts % maxSize);
            ItemEntity entity = new ItemEntity(EntityType.ITEM, level);
            entity.setItem(stack);
            entity.teleportRelative(vec3.x, vec3.y, vec3.z);
            level.addFreshEntity(entity);
        }
        if (recipe.isAnvilDamage()) event.setAnvilDamage(true);
    }

    private void blockProcess(@NotNull BlockAnvilRecipe recipe, AnvilCraftingContainer container, Level level, AnvilFallOnLandEvent event) {
        recipe.craft(container, level);
        if (recipe.isAnvilDamage()) event.setAnvilDamage(true);
    }

    private void brokeBlock(@NotNull Level level, BlockPos pos, AnvilFallOnLandEvent event) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock().getExplosionResistance() >= 1200.0) event.setAnvilDamage(true);
        if (state.getDestroySpeed(level, pos) < 0) return;
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        LootParams.Builder builder = new LootParams.Builder(serverLevel).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, false);
        dropItems(state.getDrops(builder), level, pos.getCenter());
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void redstoneEMP(Level level, BlockPos pos) {
        for (int i = -7; i < 8; i++) {
            for (int j = -7; j < 8; j++) {
                if (Math.abs(i) + Math.abs(j) > 8) continue;
                BlockPos pos1 = pos.offset(i, 0, j);
                BlockState state = level.getBlockState(pos1);
                if (!state.is(ModBlockTags.REDSTONE_TORCH)) continue;
                state = state.setValue(RedstoneTorchBlock.LIT, false);
                level.setBlock(pos1, state, 3);
            }
        }
    }

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
