package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

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
            ConfinementChamberBlockEntity.this.setChanged();
            if (ConfinementChamberBlockEntity.this.level == null || ConfinementChamberBlockEntity.this.level.isClientSide()) return;
            PacketDistributor.sendToAllPlayers(new UpdateDisplayItemPacket(
                this.getStackFrom(this.getResource(index), this.getAmountAsInt(index)),
                ConfinementChamberBlockEntity.this.getBlockPos()
            ));
            ConfinementChamberBlockEntity.this.level.sendBlockUpdated(
                ConfinementChamberBlockEntity.this.getBlockPos(), ConfinementChamberBlockEntity.this.getBlockState(),
                ConfinementChamberBlockEntity.this.getBlockState(), 3);
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
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        var items = this.itemHandler.copyToList();
        if (!items.getFirst().isEmpty()) {
            components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        ItemStack item = components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
        if (!item.isEmpty()) this.itemHandler.set(0, ItemResource.of(item), item.getCount());
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.itemHandler.serialize(output.child("Inventory"));
        return output.buildResult();
    }
}
