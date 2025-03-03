package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

public class MassInjectBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        BlockEntity blockEntity = level.getBlockEntity(hitBlockPos);
        if (!(blockEntity instanceof SpaceOvercompressorBlockEntity compressor)) return false;
        int remainingProcessCount = AnvilCraft.config.anvilEfficiency;
        long totalMassConsumed = 0L;
        RecipeManager manager = level.getRecipeManager();
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class,
            new AABB(hitBlockPos.above()),
            i -> !i.getItem().isEmpty());
        for (ItemEntity itemEntity : itemEntities) {
            Optional<MassInjectRecipe> opt = manager.getRecipeFor(ModRecipeTypes.MASS_INJECT_TYPE.get(),
                    new SingleRecipeInput(itemEntity.getItem()),
                    level)
                .map(RecipeHolder::value);
            if (opt.isEmpty()) continue;
            MassInjectRecipe recipe = opt.get();
            int count = Math.min(remainingProcessCount, itemEntity.getItem().getCount());
            remainingProcessCount -= count;
            totalMassConsumed += (long) count * recipe.getMass();
            itemEntity.getItem().shrink(count);
            if (itemEntity.getItem().isEmpty()) itemEntity.discard();
            if (remainingProcessCount <= 0) break;
        }
        compressor.injectMass(totalMassConsumed);
        return true;
    }
}
