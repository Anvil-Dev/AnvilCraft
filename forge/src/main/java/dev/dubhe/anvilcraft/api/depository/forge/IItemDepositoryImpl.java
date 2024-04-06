package dev.dubhe.anvilcraft.api.depository.forge;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.util.forge.ItemDepositoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IItemDepositoryImpl {
    @Nullable
    public static IItemDepository getItemDepository(@NotNull Level level, BlockPos pos, Direction direction) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            LazyOptional<IItemHandler> capability = be.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
            if (capability.isPresent() && capability.resolve().isPresent()) {
                return ItemDepositoryHelper.toItemDepository(capability.resolve().get());
            }
        }
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(pos));
        for (Entity entity : entities) {
            LazyOptional<IItemHandler> capability = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
            if (capability.isPresent() && capability.resolve().isPresent()) {
                return ItemDepositoryHelper.toItemDepository(capability.resolve().get());
            }
        }
        return null;
    }
}
