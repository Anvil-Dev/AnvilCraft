package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.block.entity.IExtensibleBlockEntity;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.logistics.chute.ChuteBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

@Getter
public class ChuteBlockEntity extends BaseChuteBlockEntity implements IExtensibleBlockEntity<SimpleChuteBlockEntity> {
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
    protected EnumProperty<Direction> getFacingProperty() {
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.chute");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new ChuteMenu(ModMenuTypes.CHUTE.get(), i, inventory, this);
    }

    @Override
    public BlockEntityType<SimpleChuteBlockEntity> getThatType() {
        return ModBlockEntities.SIMPLE_CHUTE.get();
    }

    @Override
    public void extend(SimpleChuteBlockEntity newBe) {
        ItemHandlerUtil.exportToTarget(this.getItemHandler(), 64, (_, _) -> true, newBe.getItemHandler());
        ItemHandlerUtil.dropAllToPos(this.getItemHandler(), newBe.getLevel(), newBe.getBlockPos().getCenter());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        ItemHandlerUtil.dropAllToPos(this.getItemHandler(), this.getLevel(), pos.getCenter());
    }
}
