package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class AutoEnchantingTableBlockEntity extends BlockEntity implements IPowerConsumer, ResourceHandler<ItemResource> {
    @Getter
    @Setter
    private PowerGrid grid;
    private int power = 16;

    @Getter
    private ItemStack inputStack = ItemStack.EMPTY;
    @Getter
    private ItemStack outputStack = ItemStack.EMPTY;
    @Getter
    @Setter
    private ItemStack prologueStack = ItemStack.EMPTY;

    public int time;
    public float rot;
    public float oldRot;
    public float targetRot;

    private final SnapshotJournal<AutoEnchantTableSnapshot> snapshotJournal = new AutoEnchantTableJournal();

    private record AutoEnchantTableSnapshot(ItemStack input, ItemStack output) {}

    private class AutoEnchantTableJournal extends SnapshotJournal<AutoEnchantTableSnapshot> {
        @Override
        protected AutoEnchantTableSnapshot createSnapshot() {
            return new AutoEnchantTableSnapshot(
                AutoEnchantingTableBlockEntity.this.inputStack.copy(),
                AutoEnchantingTableBlockEntity.this.outputStack.copy()
            );
        }

        @Override
        protected void revertToSnapshot(AutoEnchantTableSnapshot snapshot) {
            AutoEnchantingTableBlockEntity.this.inputStack = snapshot.input;
            AutoEnchantingTableBlockEntity.this.outputStack = snapshot.output;
        }

        @Override
        protected void onRootCommit(AutoEnchantTableSnapshot originalState) {
            AutoEnchantingTableBlockEntity.this.onChange();
        }
    }

    @Getter
    private final FluidStacksResourceHandler fluidHandler = new FluidStacksResourceHandler(1, 32_000) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.is(ModFluids.EXP_FLUID);
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            AutoEnchantingTableBlockEntity.this.onChange();
        }
    };

    public AutoEnchantingTableBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
    }

    public static void bookAnimationTick(
        final Level level,
        final BlockPos worldPosition,
        final BlockState state,
        final AutoEnchantingTableBlockEntity entity
    ) {
        entity.oldRot = entity.rot;
        entity.targetRot += 0.02F;

        while (entity.rot >= (float) Math.PI) {
            entity.rot -= (float) (Math.PI * 2);
        }

        while (entity.rot < (float) -Math.PI) {
            entity.rot += (float) (Math.PI * 2);
        }

        while (entity.targetRot >= (float) Math.PI) {
            entity.targetRot -= (float) (Math.PI * 2);
        }

        while (entity.targetRot < (float) -Math.PI) {
            entity.targetRot += (float) (Math.PI * 2);
        }

        float rotDir = entity.targetRot - entity.rot;

        while (rotDir >= (float) Math.PI) {
            rotDir -= (float) (Math.PI * 2);
        }

        while (rotDir < (float) -Math.PI) {
            rotDir += (float) (Math.PI * 2);
        }

        entity.rot += rotDir * 0.4F;
        entity.time++;
    }

    public static void serverTick(
        final Level level,
        final BlockPos worldPosition,
        final BlockState state,
        final AutoEnchantingTableBlockEntity entity
    ) {
        if (!entity.inputStack.isEmpty() && entity.outputStack.isEmpty()) {
            entity.outputStack = entity.inputStack.copy();
            entity.inputStack = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("input", ItemStack.OPTIONAL_CODEC, this.inputStack);
        output.store("output", ItemStack.OPTIONAL_CODEC, this.outputStack);
        output.store("prologue", ItemStack.OPTIONAL_CODEC, this.prologueStack);
        this.fluidHandler.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("input", ItemStack.OPTIONAL_CODEC).ifPresent((stack) -> this.inputStack = stack);
        input.read("output", ItemStack.OPTIONAL_CODEC).ifPresent((stack) -> this.outputStack = stack);
        input.read("prologue", ItemStack.OPTIONAL_CODEC).ifPresent((stack) -> this.prologueStack = stack);
        this.fluidHandler.deserialize(input);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), registries);
        this.saveAdditional(output);
        return output.buildResult();
    }

    private void onChange() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
        }
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public int getInputPower() {
        return this.power;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public ItemResource getResource(int index) {
        if (index == 0) {
            return ItemResource.of(this.inputStack);
        }
        if (index == 1) {
            return ItemResource.of(this.outputStack);
        }
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index == 0) {
            return this.inputStack.isEmpty() ? 0 : this.inputStack.count();
        }
        if (index == 1) {
            return this.outputStack.isEmpty() ? 0 : this.outputStack.count();
        }
        return 0;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return 1;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        if (index != 0) {
            return 0;
        }
        if (!this.inputStack.isEmpty()) {
            return 0;
        }
        this.snapshotJournal.updateSnapshots(transaction);
        this.inputStack = resource.toStack(1);
        return 1;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        return this.insert(0, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        if (index != 1) {
            return 0;
        }
        if (this.outputStack.isEmpty()) {
            return 0;
        }
        if (!ItemResource.of(this.outputStack).equals(resource)) {
            return 0;
        }
        this.snapshotJournal.updateSnapshots(transaction);
        this.outputStack = ItemStack.EMPTY;
        return 1;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        return this.extract(1, resource, amount, transaction);
    }
}
