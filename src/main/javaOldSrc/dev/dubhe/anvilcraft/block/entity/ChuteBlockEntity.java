package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class ChuteBlockEntity extends BaseChuteBlockEntity {
    protected ChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return Direction.UP == direction;
    }

    @Override
    protected boolean validateBlockState(BlockState state) {
        return state.is(ModBlocks.CHUTE.get());
    }

    @Override
    protected DirectionProperty getFacingProperty() {
        return ChuteBlock.FACING;
    }

    @Override
    protected Direction getOutputDirection() {
        return getDirection();
    }

    @Override
    protected Direction getInputDirection() {
        return Direction.UP;
    }

    @Override
    protected boolean isEnabled() {
        return getBlockState().getValue(ChuteBlock.ENABLED);
    }

    public static ChuteBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new ChuteBlockEntity(type, pos, blockState);
    }

    public static void onBlockEntityRegister(BlockEntityType<ChuteBlockEntity> type) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.anvilcraft.chute");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new ChuteMenu(ModMenuTypes.CHUTE.get(), i, inventory, this);
    }
}

