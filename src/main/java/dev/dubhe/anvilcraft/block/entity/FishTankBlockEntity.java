package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.recipe.cache.ItemResourceHandlerCache;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.PollableItemHandler;
import dev.dubhe.anvilcraft.block.workstation.FishTankBlock;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FishTankBlockEntity extends BlockEntity implements IItemResourceHandlerHolder, ItemResourceHandlerCache, IFluidHandlerHolder {
    public static final int CAPACITY = FluidType.BUCKET_VOLUME;
    private final FluidStacksResourceHandler fluidHandler = new FluidStacksResourceHandler(1, CAPACITY) {
        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            super.onContentsChanged(index, previousContents);
            FishTankBlockEntity.this.setChanged();
            if (!FishTankBlockEntity.shouldIgnite(previousContents)) FishTankBlockEntity.this.setIgnited(false);
            Level level = FishTankBlockEntity.this.getLevel();
            if (level == null) return;
            level.sendBlockUpdated(
                FishTankBlockEntity.this.getBlockPos(),
                FishTankBlockEntity.this.getBlockState(),
                FishTankBlockEntity.this.getBlockState(),
                Block.UPDATE_CLIENTS
            );
        }
    };
    /// 0-7 为输出产物，<br>
    /// 8-15 为输入物品
    private final PollableItemHandler itemHandler = new PollableItemHandler(16) {
        @Override
        public boolean isValid(int slot, ItemResource stack) {
            return slot >= 8 && slot == this.getEmptyOrSmallerSlot(stack);
        }

        @Override
        protected int getEmptyOrSmallerSlot(ItemResource resource) {
            int slot = -1;
            int countInSlot = Integer.MAX_VALUE;
            for (int i = 15; i >= 8; i--) {
                ItemResource resourceIn = this.getResource(i);
                if (!resourceIn.isEmpty() && !resourceIn.equals(resource)) continue;
                int amount = this.getAmountAsInt(i);
                if (amount <= countInSlot && amount < this.getCapacityAsInt(i, resource)) {
                    slot = i;
                    countInSlot = amount;
                }
            }
            return slot;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            super.onContentsChanged(index, previousContents);
            if (index < 8) this.tryPopoutResults(index);
            FishTankBlockEntity.this.setChanged();
            Level level = FishTankBlockEntity.this.getLevel();
            if (level == null) return;
            level.sendBlockUpdated(
                FishTankBlockEntity.this.getBlockPos(),
                FishTankBlockEntity.this.getBlockState(),
                FishTankBlockEntity.this.getBlockState(),
                Block.UPDATE_CLIENTS
            );
        }

        private void tryPopoutResults(int index) {
            if (!FishTankBlockEntity.this.getBlockState().getValue(FishTankBlock.OUTLET)) return;
            ItemResource resource = this.getResource(index);
            if (resource.isEmpty()) return;
            Direction outletDir = getBlockState().getValue(FishTankBlock.FACING);
            if (FishTankBlockEntity.this.level == null) return;
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = this.extract(index, resource, Integer.MAX_VALUE, transaction);
                if (extracted == 0) return;
                FishTankBlockEntity.popResource(
                    FishTankBlockEntity.this.level,
                    FishTankBlockEntity.this.getBlockPos(),
                    outletDir,
                    resource.toStack(extracted)
                );
            }
        }
    };
    /// 输出产物的存储代理，用于炼药锅配方输出
    private final PollableItemHandler outputProxy = new PollableItemHandler(8) {
        @Override
        public ItemResource getResource(int index) {
            return FishTankBlockEntity.this.itemHandler.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return FishTankBlockEntity.this.itemHandler.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return FishTankBlockEntity.this.itemHandler.getCapacityAsLong(index, resource);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            FishTankBlockEntity.this.itemHandler.set(index, this.getResource(index), this.getAmountAsInt(index));
        }
    };
    private boolean ignited = false;

    public FishTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setIgnited(boolean ignited) {
        this.ignited = ignited;
        this.setChanged();
        Level level = this.getLevel();
        if (level == null) return;
        level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public PollableItemHandler getInput() {
        return this.itemHandler;
    }

    public PollableItemHandler getOutput() {
        return this.outputProxy;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        ItemHandlerUtil.dropAllToPos(this.getItemHandler(), this.level, pos.getCenter());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.fluidHandler.serialize(output.child("Fluid"));
        this.itemHandler.serialize(output.child("Items"));
        output.putBoolean("ignited", this.ignited);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.fluidHandler.deserialize(input.childOrEmpty("Fluid"));
        this.itemHandler.deserialize(input.childOrEmpty("Items"));
        this.ignited = input.getBooleanOr("ignited", false)
                       && FishTankBlockEntity.shouldIgnite(this.fluidHandler.getResource(0).toStack(this.fluidHandler.getAmountAsInt(0)));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), registries);
        this.fluidHandler.serialize(output.child("Fluid"));
        this.itemHandler.serialize(output.child("Items"));
        tag.merge(output.buildResult());
        tag.putBoolean("ignited", this.ignited);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean onPlayerUse(Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.fluidHandler)) return true;
        ItemStack inHand = player.getItemInHand(hand);
        if (inHand.isEmpty()) {
            if (hand != InteractionHand.MAIN_HAND) return false;
            List<ItemStack> stacks = FishTankBlockEntity.extractAllFromTank(this.itemHandler, TriState.TRUE);
            if (stacks.isEmpty()) return false;
            for (ItemStack stack : stacks) {
                player.getInventory().placeItemBackInInventory(stack);
            }
            return true;
        } else {
            if (hitResult.getLocation().y - hitResult.getBlockPos().getY() < 5 / 8F) return false;
            return FishTankBlockEntity.insertToTank(this.itemHandler, inHand);
        }
    }

    /**
     * 向鱼缸中放入物品
     *
     * @param handler 鱼缸物品处理器
     * @param entity  要放入的物品实体
     */
    public static void insertToTank(
        @Nullable ResourceHandler<ItemResource> handler,
        IndexModifier<ItemResource> modifier,
        ItemEntity entity
    ) {
        ItemStack stack = entity.getItem();
        if (entity.anvilcraft$isAdsorbable()) {
            FishTankBlockEntity.insertToTank(handler, stack);
            return;
        }
        int remaining = stack.getCount();
        while (remaining > 0) {
            int slot = -1;
            ItemResource resource = null;
            int limit = 0;
            int existing = 0;
            for (int i = 0; i < 8; i++) {
                resource = handler.getResource(i);
                if (!resource.isEmpty() && !resource.matches(stack)) continue;
                limit = Math.min(
                    resource.isEmpty() ? Item.DEFAULT_MAX_STACK_SIZE : resource.getMaxStackSize(),
                    handler.getCapacityAsInt(slot, resource)
                );
                existing = handler.getAmountAsInt(i);
                if (existing >= limit) continue;
                slot = i;
                break;
            }
            if (slot < 0) return;
            entity.discard();
            int storing = Math.min(remaining, limit - existing);
            remaining -= storing;
            modifier.set(slot, resource, existing + storing);
        }
    }

    /**
     * 向鱼缸中放入物品
     *
     * @param handler 鱼缸物品处理器
     * @param stack 要放入的物品
     * @return 是否放入成功
     */
    public static boolean insertToTank(@Nullable ResourceHandler<ItemResource> handler, ItemStack stack) {
        if (handler == null) return false;
        if (stack.is(ModItemTags.DISALLOW_HAND_INSERT_INTO_TANK)) return false;
        int count = stack.getCount();
        ItemResource resource = ItemResource.of(stack);
        try (Transaction root = Transaction.openRoot()) {
            for (int i = 8; i < 16; i++) {
                try (Transaction transaction = Transaction.open(root)) {
                    int inserted = handler.insert(i, resource, count, transaction);
                    if (inserted == 0) continue;
                    handler.insert(i, resource, inserted, transaction);
                    count -= inserted;
                    transaction.commit();
                }
                if (count <= 0) return true;
            }
            root.commit();
        }
        return stack.getCount() != count;
    }

    /**
     * 从鱼缸中提取出所有物品
     *
     * @param handler 鱼缸物品处理器
     * @param containsIngredient 是否同时提取原料；<br>
     *        {@link TriState#DEFAULT DEFAULT}为始终提取，<br>
     *        {@link TriState#TRUE TRUE}为仅在产物为空时提取，<br>
     *        {@link TriState#FALSE FALSE}为不提取
     * @return 提取出的所有物品
     */
    public static @Unmodifiable List<ItemStack> extractAllFromTank(ResourceHandler<ItemResource> handler, TriState containsIngredient) {
        List<ItemStack> result = new ArrayList<>();
        try (Transaction root = Transaction.openRoot()) {
            for (int i = 0; i < 8; i++) {
                ItemResource resource = handler.getResource(i);
                int maxSize = resource.getMaxStackSize();
                try (Transaction transaction = Transaction.open(root)) {
                    int extracted = handler.extract(i, resource, Integer.MAX_VALUE, transaction);
                    if (extracted == 0) continue;
                    if (extracted < maxSize) {
                        result.add(resource.toStack(extracted));
                        transaction.commit();
                        continue;
                    }
                    for (; extracted > 0; extracted -= maxSize) {
                        result.add(resource.toStack(Math.min(extracted, maxSize)));
                    }
                }
            }
            root.commit();
        }
        if (!containsIngredient.isFalse() && (containsIngredient.isDefault() || result.isEmpty())) {
            try (Transaction root = Transaction.openRoot()) {
                for (int i = 8; i < 16; i++) {
                    ItemResource resource = handler.getResource(i);
                    int maxSize = resource.getMaxStackSize();
                    try (Transaction transaction = Transaction.open(root)) {
                        int extracted = handler.extract(i, resource, Integer.MAX_VALUE, transaction);
                        if (extracted == 0) continue;
                        if (extracted < maxSize) {
                            result.add(resource.toStack(extracted));
                            transaction.commit();
                            continue;
                        }
                        for (; extracted > 0; extracted -= maxSize) {
                            result.add(resource.toStack(Math.min(extracted, maxSize)));
                        }
                    }
                }
                root.commit();
            }
        }
        return ImmutableList.copyOf(result);
    }

    public static boolean shouldIgnite(FluidStack cur) {
        return cur.is(ModFluidTags.IGNITABLE);
    }

    private static void popResource(Level level, BlockPos pos, Direction direction, ItemStack stack) {
        int stepX = direction.getStepX();
        int stepZ = direction.getStepZ();
        double halfWidth = (double) EntityType.ITEM.getWidth() / 2.0;
        double posX = (double) pos.getX() + 0.5
                      + (stepX == 0 ? Mth.nextDouble(level.getRandom(), -0.25, 0.25) : (double) stepX * (0.5 + halfWidth));
        double posY = pos.getY() + 0.5;
        double posZ = (double) pos.getZ() + 0.5
                      + (stepZ == 0 ? Mth.nextDouble(level.getRandom(), -0.25, 0.25) : (double) stepZ * (0.5 + halfWidth));
        double deltaX = stepX == 0 ? Mth.nextDouble(level.getRandom(), -0.1, 0.1) : (double) stepX * 0.1;
        double deltaY = Mth.nextDouble(level.getRandom(), 0.0, 0.1);
        double deltaZ = stepZ == 0 ? Mth.nextDouble(level.getRandom(), -0.1, 0.1) : (double) stepZ * 0.1;
        ItemEntity entity = new ItemEntity(level, posX, posY, posZ, stack, deltaX, deltaY, deltaZ);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}
