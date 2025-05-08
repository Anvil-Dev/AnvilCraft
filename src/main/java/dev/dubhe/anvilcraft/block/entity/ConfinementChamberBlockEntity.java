package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class ConfinementChamberBlockEntity extends BlockEntity implements IItemHandlerHolder, IHasDisplayItem {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    @Getter
    private final int id;

    public ConfinementChamberBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CONFINEMENT_CHAMBER.get(), pos, blockState);
        this.id = COUNTER.incrementAndGet();
    }

    private ConfinementChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.id = -1;
    }

    public static ConfinementChamberBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new ConfinementChamberBlockEntity(type, pos, blockState);
    }

    @Getter
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (level == null || level.isClientSide) return;
            PacketDistributor.sendToAllPlayers((new UpdateDisplayItemPacket(getStackInSlot(slot), getBlockPos())));
        }
    };

    @Override
    public void updateDisplayItem(ItemStack stack) {
        itemHandler.setStackInSlot(0, stack);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Inventory", itemHandler.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.loadAdditional(tag, provider);
        itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }
}
