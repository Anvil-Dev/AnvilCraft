package dev.dubhe.anvilcraft.block.entity.forge;

import dev.dubhe.anvilcraft.api.depository.forge.ItemDepositoryHelperImpl;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrabTrapBlockEntityImpl extends CrabTrapBlockEntity {
    public CrabTrapBlockEntityImpl(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static CrabTrapBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new CrabTrapBlockEntityImpl(type, pos, blockState);
    }

    public static void onBlockEntityRegister(BlockEntityType<CrabTrapBlockEntity> type) {
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap,
                LazyOptional.of(() -> ItemDepositoryHelperImpl.toItemHandler(getDepository())));
        }
        return super.getCapability(cap, side);
    }
}
