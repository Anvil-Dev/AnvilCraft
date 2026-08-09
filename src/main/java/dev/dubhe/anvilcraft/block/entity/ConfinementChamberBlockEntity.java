package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class ConfinementChamberBlockEntity extends BlockEntity implements IItemResourceHandlerHolder, IHasDisplayItem {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    @Getter
    private final int id;

    public ConfinementChamberBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CONFINEMENT_CHAMBER.get(), pos, blockState);
        this.id = ConfinementChamberBlockEntity.COUNTER.incrementAndGet();
    }

    private ConfinementChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.id = -1;
    }

    public static ConfinementChamberBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new ConfinementChamberBlockEntity(type, pos, blockState);
    }

    @Getter
    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            super.onContentsChanged(index, previousContents);
            if (ConfinementChamberBlockEntity.this.level == null || ConfinementChamberBlockEntity.this.level.isClientSide()) return;
            PacketDistributor.sendToAllPlayers(new UpdateDisplayItemPacket(
                this.getStackFrom(this.getResource(index), this.getAmountAsInt(index)),
                ConfinementChamberBlockEntity.this.getBlockPos()
            ));
        }
    };

    @Override
    public void updateDisplayItem(ItemStack stack) {
        this.itemHandler.set(0, ItemResource.of(stack), stack.getCount());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.itemHandler.serialize(output.child("Inventory"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Inventory").ifPresent(this.itemHandler::deserialize);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.level;
        if (level != null) {
            Containers.dropContents(level, pos, this.itemHandler.copyToList());
        }
    }
}
