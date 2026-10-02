package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.recipe.cache.ItemResourceHandlerCache;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 加工台方块实体公共父类：冲压台、粉碎台、过筛台、拆包台的原料存储与交互逻辑。
 */
@Getter
public abstract class ProcessingTableBlockEntity extends BlockEntity implements IItemResourceHandlerHolder, ItemResourceHandlerCache {
    public static final int INPUT_SLOTS = 8;

    private final ItemStacksResourceHandler input = new ItemStacksResourceHandler(INPUT_SLOTS) {
        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            int sameItemCount = 0;
            for (int index = 0; index < this.size(); index++) {
                if (this.getResource(index).is(resource.getItem())) sameItemCount += this.getAmountAsInt(index);
            }
            int accepted = Math.min(resource.getMaxStackSize() - sameItemCount, amount);
            return accepted <= 0 ? 0 : super.insert(slot, resource, accepted, transaction);
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            ProcessingTableBlockEntity.this.setChanged();
            ProcessingTableBlockEntity.this.sendUpdate();
        }

        @Override
        public void deserialize(ValueInput input) {
            var contents = NonNullList.withSize(INPUT_SLOTS, ItemStack.EMPTY);
            var nativeStacks = input.read("stacks", ItemStack.OPTIONAL_CODEC.listOf());
            if (nativeStacks.isPresent()) {
                var saved = nativeStacks.get();
                for (int slot = 0; slot < Math.min(saved.size(), INPUT_SLOTS); slot++) contents.set(slot, saved.get(slot));
            } else {
                for (ItemStackWithSlot saved : input.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
                    if (saved.isValidInContainer(INPUT_SLOTS)) contents.set(saved.slot(), saved.stack());
                }
            }
            this.setStacks(contents);
        }
    };

    private final ResourceHandler<ItemResource> proxy = new DelegatingResourceHandler<>(this.input) {
        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            Objects.checkIndex(index, this.size());
            return this.extract(resource, amount, transaction);
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }
    };

    private long doorStartTick;
    private int doorDurationTick;

    protected ProcessingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.proxy;
    }

    @Override
    public ResourceHandler<ItemResource> getInput() {
        return this.input;
    }

    @Override
    public ResourceHandler<ItemResource> getOutput() {
        return EMPTY_OUTPUT;
    }

    private void sendUpdate() {
        if (this.level == null || this.level.isClientSide()) return;
        this.level.sendBlockUpdated(
            this.getBlockPos(),
            this.getBlockState(),
            this.getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.saveAdditional(output);
        return output.buildResult();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * 手持物品时将手中物品插入原料槽，空手时取出全部内容物。
     *
     * @return 是否完成了交互
     */
    public boolean tryInteractItems(Player player, InteractionHand hand) {
        if (this.level == null || hand != InteractionHand.MAIN_HAND) return false;
        ItemStack inHand = player.getItemInHand(hand);
        if (inHand.isEmpty()) {
            if (this.level.isClientSide()) {
                for (int slot = 0; slot < this.input.size(); slot++) {
                    if (!this.input.getResource(slot).isEmpty()) return true;
                }
                return false;
            }
            List<ItemStack> stacks = new ArrayList<>();
            this.extractAllItems(stacks);
            if (stacks.isEmpty()) return false;
            Inventory inventory = player.getInventory();
            for (ItemStack stack : stacks) {
                inventory.placeItemBackInInventory(stack);
            }
            return true;
        }
        if (this.level.isClientSide()) return true;
        ItemStack remaining = ItemHandlerUtil.insertItem(this.input, inHand.copy(), false);
        int count = inHand.getCount();
        inHand.setCount(remaining.getCount());
        return remaining.getCount() != count;
    }

    private void extractAllItems(List<ItemStack> stacks) {
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < this.input.size(); slot++) {
                ItemResource resource = this.input.getResource(slot);
                if (resource.isEmpty()) continue;
                int amount = this.input.extract(slot, resource, Integer.MAX_VALUE, transaction);
                if (amount > 0) stacks.add(resource.toStack(amount));
            }
            transaction.commit();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) this.dropAllContent(this.level, pos);
    }

    public void dropAllContent(Level level, BlockPos pos) {
        List<ItemStack> stacks = new ArrayList<>();
        this.extractAllItems(stacks);
        for (ItemStack stack : stacks) {
            Block.popResource(level, pos, stack);
        }
    }

    /**
     * 加工台执行配方后激活底部双开门动画。
     *
     * @param durationTick 动画持续时长，单位 tick
     */
    public void onRecipeExecuted(int durationTick) {
        Level level = this.getLevel();
        if (level == null) return;
        this.doorStartTick = level.getGameTime();
        this.doorDurationTick = durationTick;
        this.setChanged();
        this.sendUpdate();
    }

    /**
     * 获取开门动画进度，用于渲染配方执行时底部双开门的开合程度。
     *
     * @param partialTick 部分进度
     * @return 开门进度，范围 [0,1]
     */
    public float getDoorOpenProgress(float partialTick) {
        Level level = this.getLevel();
        if (level == null || this.doorDurationTick <= 0) return 0;
        long elapsed = level.getGameTime() - this.doorStartTick;
        if (elapsed >= this.doorDurationTick) return 0;
        float progress = Math.max(0, (elapsed + partialTick) / this.doorDurationTick);
        return 1 - Math.abs(2 * progress - 1);
    }

    /**
     * 获取磨轮旋转进度，用于渲染动画。
     *
     * @param partialTick 部分进度
     * @return 旋转进度，范围 [0,1]，1 表示已停止
     */
    public float getSpinProgress(float partialTick) {
        Level level = this.getLevel();
        if (level == null || this.doorDurationTick <= 0) return 1;
        long elapsed = level.getGameTime() - this.doorStartTick;
        if (elapsed >= this.doorDurationTick) return 1;
        return (elapsed + partialTick) / this.doorDurationTick;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.input.serialize(output.child("Inputs"));
        output.putLong("DoorStartTick", this.doorStartTick);
        output.putInt("DoorDurationTick", this.doorDurationTick);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.input.deserialize(input.childOrEmpty("Inputs"));
        this.doorStartTick = input.getLongOr("DoorStartTick", 0);
        this.doorDurationTick = input.getIntOr("DoorDurationTick", 0);
    }

    private static final ResourceHandler<ItemResource> EMPTY_OUTPUT = new ItemStacksResourceHandler(0);
}
