package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.entity.IExperienceOrbExtension;
import dev.dubhe.anvilcraft.block.ExpCollectorBlock;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Map;

@Mixin(ExperienceOrb.class)
abstract class ExperienceOrbMixin extends Entity implements IExperienceOrbExtension {
    @Shadow
    public int count;
    @Shadow
    public int value;
    @Unique
    public boolean anvilcraft$discarded = false;

    @Unique
    public boolean anvilcraft$shouldPoach = true;

    public ExperienceOrbMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    @Override
    public void anvilcraft$poach() {
        if (!anvilcraft$shouldPoach) return;
        Level level = this.level();
        if (level.isClientSide) return;
        Map<ChunkPos, List<ExpCollectorBlockEntity>> map = ExpCollectorBlockEntity.POACHING_COLLECTOR_MAP.get(level);
        if (map == null) return;
        ChunkPos chunkPos = this.chunkPosition();
        List<ExpCollectorBlockEntity> list = map.get(chunkPos);
        if (list == null || list.isEmpty()) return;
        boolean flag = false;
        for (ExpCollectorBlockEntity collector : list) {
            if (collector.isGridWorking()
                && !collector.getBlockState().getValue(ExpCollectorBlock.POWERED)
                && collector.shape().contains(this.position())
                && !collector.isRemoved()) {
                int count = this.count;
                int value = this.value;
                int totalExp = count * value;
                int mb = value * 20;
                int totalMB = totalExp * 20;
                IFluidHandler fluidHandler = collector.getFluidHandler();
                if (fluidHandler.getTankCapacity(0) - fluidHandler.getFluidInTank(0).getAmount() >= mb) {
                    fluidHandler.fill(new FluidStack(ModFluids.EXP_FLUID, totalExp), IFluidHandler.FluidAction.EXECUTE);
                    level.sendBlockUpdated(collector.getBlockPos(), collector.getBlockState(), collector.getBlockState(), Block.UPDATE_ALL);
                    flag = true;
                } else {
                    while (fluidHandler.getTankCapacity(0) - fluidHandler.getFluidInTank(0).getAmount() >= totalMB) {
                        fluidHandler.fill(new FluidStack(ModFluids.EXP_FLUID, value), IFluidHandler.FluidAction.EXECUTE);
                        level.sendBlockUpdated(
                            collector.getBlockPos(),
                            collector.getBlockState(),
                            collector.getBlockState(),
                            Block.UPDATE_ALL
                        );
                        this.count--;
                        if (this.count < 1) {
                            flag = true;
                            break;
                        }
                    }
                }
            }
        }
        if (flag) {
            this.remove(Entity.RemovalReason.DISCARDED);
            this.discard();
            anvilcraft$discarded = true;
        }
    }

    @Unique
    @Override
    public void anvilcraft$setShouldPoach(boolean shouldPoach) {
        this.anvilcraft$shouldPoach = shouldPoach;
    }
}
