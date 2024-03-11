package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.block.BlockAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

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
}
