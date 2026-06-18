package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.block.entity.IConvertableBlockEntity;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.logistics.chute.ChuteBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ChuteBlockEntity extends BaseChuteBlockEntity implements IConvertableBlockEntity<SimpleChuteBlockEntity> {
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
    public Holder<BlockEntityType<?>> targetTypeHolder() {
        return ModBlockEntities.SIMPLE_CHUTE;
    }

    @Override
    public void convertTo(SimpleChuteBlockEntity newBe) {
        List<ItemStack> drops = new ArrayList<>();
        FilteredItemStackHandler handler = this.getItemHandler();
        for (int i = 0; i < handler.size(); i++) {
            ItemResource resource = handler.getResource(i);
            if (resource.isEmpty()) {
                continue;
            }
            int count = handler.getAmountAsInt(i);
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = newBe.getItemHandler().insert(resource, count, transaction);
                int extracted = handler.extract(i, resource, inserted, transaction);
                if (inserted > extracted) {
                    continue;
                }
                count -= inserted;
                if (count > 0) {
                    drops.add(resource.toStack(count));
                }
                transaction.commit();
            }
        }
        AnvilUtil.dropItems(drops, this.level, newBe.getBlockPos().getCenter());
    }
}
