package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.fluid.HoneyFluid;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FishTankCompressBehavior implements IAnvilBehavior {
    private static final Map<Class<?>, List<ItemStack>> FLUID_OUTPUTS = new LinkedHashMap<>();

    static {
        FLUID_OUTPUTS.put(dev.dubhe.anvilcraft.fluid.MilkFluid.class, List.of(
            new ItemStack(ModFoodItems.CREAM.get(), 4)
        ));
        FLUID_OUTPUTS.put(HoneyFluid.class, List.of(
            new ItemStack(Items.HONEY_BLOCK)
        ));
    }

    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (!hitBlockState.is(ModBlocks.FISH_TANK)) return false;
        if (!(level.getBlockEntity(hitBlockPos) instanceof FishTankBlockEntity fishTank)) return false;

        var drained = fishTank.getFluidHandler().drain(
            FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE
        );
        if (drained.getAmount() != FluidType.BUCKET_VOLUME) return false;

        List<ItemStack> outputs = null;
        for (Map.Entry<Class<?>, List<ItemStack>> entry : FLUID_OUTPUTS.entrySet()) {
            if (entry.getKey().isInstance(drained.getFluid())) {
                outputs = entry.getValue();
                break;
            }
        }
        if (outputs == null) return false;

        if (level instanceof ServerLevel) {
            fishTank.getFluidHandler().drain(
                FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE
            );

            var handler = fishTank.getItemHandler();
            for (ItemStack output : outputs) {
                for (int slot = 0; slot < 8; slot++) {
                    ItemStack existing = handler.getStackInSlot(slot);
                    if (existing.isEmpty()) {
                        handler.setStackInSlot(slot, output.copy());
                        break;
                    }
                    if (ItemStack.isSameItemSameComponents(existing, output)
                        && existing.getCount() + output.getCount() <= existing.getMaxStackSize()
                    ) {
                        existing.grow(output.getCount());
                        break;
                    }
                }
            }

            level.playSound(null, hitBlockPos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS);
        }
        return true;
    }
}
