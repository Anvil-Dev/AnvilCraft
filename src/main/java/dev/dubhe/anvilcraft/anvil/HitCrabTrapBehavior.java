package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.loot.ModLootTables;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HitCrabTrapBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (level.isClientSide()) {
            return false;
        }
        if (hitBlockState.is(ModBlocks.CRAB_TRAP)) {
            List<ItemStack> items = new ObjectArrayList<>();
            for (int i = 0; i < hitBlockState.getValue(CrabTrapBlock.FINISHING); i++) {
                items.addAll(CrabTrapBlock.generateLoot((ServerLevel) level, hitBlockPos, ModLootTables.CRAB_TRAP_COMMON));
                items.addAll(CrabTrapBlock.generateLoot((ServerLevel) level, hitBlockPos, ModLootTables.CRAB_TRAP_RIVER));
                items.addAll(CrabTrapBlock.generateLoot((ServerLevel) level, hitBlockPos, ModLootTables.CRAB_TRAP_OCEAN));
                items.addAll(CrabTrapBlock.generateLoot((ServerLevel) level, hitBlockPos, ModLootTables.CRAB_TRAP_WARM_OCEAN));
                items.addAll(CrabTrapBlock.generateLoot((ServerLevel) level, hitBlockPos, ModLootTables.CRAB_TRAP_SWAMP));
                items.addAll(CrabTrapBlock.generateLoot((ServerLevel) level, hitBlockPos, ModLootTables.CRAB_TRAP_JUNGLE));
            }
            for (int i = 0; i <= hitBlockState.getValue(CrabTrapBlock.FINISHING); i++) {
                if (i % 5 == 0) {
                    items.add(ModItems.CRAB_CLAW.asStack());
                }
            }
            Vec3 dropPos = hitBlockPos
                .above()
                .relative(hitBlockState.getValue(CrabTrapBlock.FACING))
                .getCenter()
                .relative(hitBlockState.getValue(CrabTrapBlock.FACING).getOpposite(), 0.5);
            for (ItemStack item : items) {
                ItemEntity itemEntity = new ItemEntity(level, dropPos.x, dropPos.y - 0.4, dropPos.z, item, 0, 0, 0);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
            level.setBlockAndUpdate(hitBlockPos, hitBlockState.setValue(CrabTrapBlock.FINISHING, 0));
            return true;
        }
        return false;
    }
}
