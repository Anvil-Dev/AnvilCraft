package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.recipe.cache.IItemHandlerCache;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.PollableItemHandler;
import dev.dubhe.anvilcraft.block.ExpFluidBlock;
import dev.dubhe.anvilcraft.block.FishTankBlock;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

@Getter
public class FishTankBlockEntity extends BlockEntity implements IItemHandlerHolder, IItemHandlerCache, IFluidHandlerHolder {
    private static final double EPSILON = 1.0 / 1024.0;
    public static final int CAPACITY = FluidType.BUCKET_VOLUME;
    public static final int MAX_TROPICAL_FISH = 4;

    private static final Vec3 FLUID_CONTENT_AREA_MIN = new Vec3(0.0625, 0.0625, 0.0625);
    private static final Vec3 FLUID_CONTENT_AREA_MAX = new Vec3(0.9375, 0.9375, 0.9375);
    private static final double FLUID_CONTENT_AREA_HEIGHT = 7.0 / 8;
    private static final String TAG_TROPICAL_FISH_DATA = "TropicalFishData";

    // region private final List<CompoundTag> tropicalFishData = new ArrayList<>();
    private final List<CompoundTag> tropicalFishData = new ArrayList<>() {
        @Override
        public boolean add(CompoundTag tag) {
            if (this.size() >= MAX_TROPICAL_FISH) return false;
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.sendUpdate();
            FishTankBlockEntity.this.sendNeighbourUpdate();
            return super.add(tag);
        }

        @Override
        public CompoundTag removeLast() {
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.sendUpdate();
            FishTankBlockEntity.this.sendNeighbourUpdate();
            return super.removeLast();
        }

        @Override
        public void clear() {
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.sendUpdate();
            FishTankBlockEntity.this.sendNeighbourUpdate();
            super.clear();
        }
    };
    // endregion
    private AABB fluidContentArea = new AABB(FLUID_CONTENT_AREA_MIN, FLUID_CONTENT_AREA_MAX);
    // region private final FluidTank fluidHandler = new FluidTank(FishTankBlockEntity.CAPACITY);
    private final FluidTank fluidHandler = new FluidTank(FishTankBlockEntity.CAPACITY) {
        @Override
        protected void onContentsChanged() {
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.refreshIgnited();
            FishTankBlockEntity.this.sendUpdate();
            FishTankBlockEntity.this.sendNeighbourUpdate();
            this.updateContentArea();
            if (this.getFluid().is(Fluids.WATER)) return;

            FishTankBlockEntity.this.dropFish();
            FishTankBlockEntity.this.updateFishState();
        }

        @Override
        public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
            FluidTank tank = super.readFromNBT(lookupProvider, nbt);
            this.onContentsChanged();
            return tank;
        }

        private void updateContentArea() {
            double diffY = FishTankBlockEntity.FLUID_CONTENT_AREA_HEIGHT * (1.0 - (double) this.getFluidAmount() / this.getCapacity());
            Vec3 pos = getBlockPos().getBottomCenter().subtract(0.5, 0, 0.5);
            FishTankBlockEntity.this.fluidContentArea = new AABB(
                FishTankBlockEntity.FLUID_CONTENT_AREA_MIN.add(pos),
                FishTankBlockEntity.FLUID_CONTENT_AREA_MAX.subtract(0, diffY, 0).add(pos)
            );
        }
    };
    // endregion

    /**
     * 0-7 为输出产物，<br>
     * 8-15 为输入原料
     */
    // region private final ItemStackHandler proxy = new ItemStackHandler(16);
    private final ItemStackHandler proxy = new ItemStackHandler(16) {
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 8) {
                return FishTankBlockEntity.this.output.getStackInSlot(slot);
            } else {
                return FishTankBlockEntity.this.input.getStackInSlot(slot - 8);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < 8) {
                return FishTankBlockEntity.this.output.getSlotLimit(slot);
            } else {
                return FishTankBlockEntity.this.input.getSlotLimit(slot - 8);
            }
        }

        @Override
        public void setSize(int size) {
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 8) {
                FishTankBlockEntity.this.output.setStackInSlot(slot, stack);
            } else {
                FishTankBlockEntity.this.input.setStackInSlot(slot - 8, stack);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 8 && FishTankBlockEntity.this.input.isItemValid(slot - 8, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            if (!this.isItemValid(slot, stack)) return stack;
            this.validateSlotIndex(slot);

            ItemStack existing = this.getStackInSlot(slot);
            int limit = this.getStackLimit(slot, stack);

            if (!existing.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(stack, existing)) return stack;

                limit -= existing.getCount();
            }

            if (limit <= 0) return stack;

            boolean reachedLimit = stack.getCount() > limit;
            if (!simulate) {
                if (existing.isEmpty()) {
                    this.setStackInSlot(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
                } else {
                    existing.grow(reachedLimit ? limit : stack.getCount());
                }
                this.onContentsChanged(slot);
            }
            return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount == 0) return ItemStack.EMPTY;
            this.validateSlotIndex(slot);

            ItemStack existing = this.getStackInSlot(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;

            int toExtract = Math.min(amount, existing.getMaxStackSize());
            if (existing.getCount() <= toExtract) {
                if (!simulate) {
                    this.setStackInSlot(slot, ItemStack.EMPTY);
                    this.onContentsChanged(slot);
                    return existing;
                } else {
                    return existing.copy();
                }
            } else {
                if (!simulate) {
                    this.setStackInSlot(slot, existing.copyWithCount(existing.getCount() - toExtract));
                    this.onContentsChanged(slot);
                }

                return existing.copyWithCount(toExtract);
            }
        }
    };
    // endregion
    // region private final PollableItemHandler input = new PollableItemHandler(8);
    private final PollableItemHandler input = new PollableItemHandler(8) {
        @Override
        protected int getEmptyOrSmallerSlot(ItemStack stack) {
            int slot = -1;
            int countInSlot = Integer.MAX_VALUE;
            for (int i = 7; i >= 0; i--) {
                ItemStack stackInSlot = this.getStackInSlot(i);
                if (stackInSlot.isEmpty()) {
                    slot = i;
                    continue;
                }
                if (!ItemStack.isSameItemSameComponents(stackInSlot, stack)) continue;
                int stackInSlotCount = stackInSlot.getCount();
                if (stackInSlotCount <= countInSlot && stackInSlotCount < this.getSlotLimit(i)) {
                    slot = i;
                    countInSlot = stackInSlotCount;
                }
            }
            return slot;
        }

        @Override
        protected void onContentsChanged(int slot) {
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.refreshIgnited();
            FishTankBlockEntity.this.sendUpdate();
        }
    };
    // endregion
    // region private final ItemStackHandler output = new ItemStackHandler(8);
    private final ItemStackHandler output = new ItemStackHandler(8) {
        private boolean autoOutputting = false;

        @Override
        protected void onContentsChanged(int slot) {
            if (!this.autoOutputting) this.checkAutoOutput(slot);
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.refreshIgnited();
            FishTankBlockEntity.this.sendUpdate();
        }

        void checkAutoOutput(int slot) {
            Level level = FishTankBlockEntity.this.level;
            if (level == null || level.isClientSide()) return;
            BlockState state = FishTankBlockEntity.this.getBlockState();
            if (!state.getValue(FishTankBlock.OUTLET)) return;
            ItemStack stack = this.extractItem(slot, Integer.MAX_VALUE, true);
            if (stack.isEmpty()) return;
            Direction outletDir = state.getValue(FishTankBlock.FACING);

            BlockPos pos = FishTankBlockEntity.this.getBlockPos();
            List<IItemHandler> targets = ItemHandlerUtil.getTargetItemHandlerList(pos.relative(outletDir), null, level);
            if (targets == null || targets.isEmpty()) {
                FishTankBlockEntity.popResourceFromFace(level, pos, outletDir, this.extractItem(slot, Integer.MAX_VALUE, false));
                return;
            }
            int remaining = stack.getCount();
            for (IItemHandler target : targets) {
                ItemStack remainingCache = stack.copyWithCount(remaining);
                if (ItemHandlerUtil.insertItem(target, remainingCache, true).getCount() == remaining) continue;
                remaining = ItemHandlerUtil.insertItem(target, remainingCache, false).getCount();
                if (remaining == 0) break;
            }
            this.autoOutputting = true;
            this.setStackInSlot(slot, stack.copyWithCount(remaining));
            this.autoOutputting = false;
        }
    };
    // endregion
    private boolean ignited = false;

    public FishTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.updateFishState();
    }

    private void sendUpdate() {
        if (this.level == null) return;
        this.level.sendBlockUpdated(
            this.getBlockPos(),
            this.getBlockState(),
            this.getBlockState(),
            Block.UPDATE_ALL
        );
    }

    private void sendNeighbourUpdate() {
        if (this.level != null) this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
    }

    @Override
    public ItemStackHandler getItemHandler() {
        return this.proxy;
    }

    // region 持久化
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag tankNbt = this.fluidHandler.writeToNBT(registries, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("Fluid", tankNbt);
        }
        CompoundTag inputNbt = this.input.serializeNBT(registries);
        if (!inputNbt.isEmpty()) {
            tag.put("Inputs", inputNbt);
        }
        CompoundTag outputNbt = this.output.serializeNBT(registries);
        if (!outputNbt.isEmpty()) {
            tag.put("Outputs", outputNbt);
        }
        tag.putBoolean("ignited", this.ignited);

        if (!this.tropicalFishData.isEmpty()) {
            ListTag list = new ListTag();
            for (CompoundTag fishTag : this.tropicalFishData) {
                list.add(fishTag.copy());
            }
            tag.put(TAG_TROPICAL_FISH_DATA, list);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.fluidHandler.readFromNBT(registries, tag.getCompound("Fluid"));
        // TODO: 兼容性支持后删除以下判断
        if (tag.contains("Items")) {
            CompoundTag full = tag.getCompound("Items");
            ListTag items = full.getList("Items", Tag.TAG_COMPOUND);

            ListTag inputItems = new ListTag();
            for (int i = 0; i < 8; i++) {
                inputItems.add(items.getCompound(i));
            }
            CompoundTag input = full.copy();
            input.put("Items", inputItems);
            this.input.deserializeNBT(registries, input);

            ListTag outputItems = new ListTag();
            for (int i = 8; i < 16; i++) {
                CompoundTag item = items.getCompound(i);
                item.putInt("Slot", item.getInt("Slot") - 8);
                outputItems.add(item);
            }
            CompoundTag output = full.copy();
            output.put("Items", outputItems);
            this.output.deserializeNBT(registries, input);
        }
        this.input.deserializeNBT(registries, tag.getCompound("Inputs"));
        this.output.deserializeNBT(registries, tag.getCompound("Outputs"));
        this.ignited = tag.getBoolean("ignited") && FishTankBlockEntity.canIgnite(this.fluidHandler.getFluid());

        this.tropicalFishData.clear();
        if (tag.contains(TAG_TROPICAL_FISH_DATA, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_TROPICAL_FISH_DATA, Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(MAX_TROPICAL_FISH, list.size()); i++) {
                this.tropicalFishData.add(list.getCompound(i).copy());
            }
        }
    }
    // endregion

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag tankNbt = this.fluidHandler.writeToNBT(registries, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("Fluid", tankNbt);
        }
        CompoundTag inputNbt = this.input.serializeNBT(registries);
        if (!inputNbt.isEmpty()) {
            tag.put("Inputs", inputNbt);
        }
        CompoundTag outputNbt = this.output.serializeNBT(registries);
        if (!outputNbt.isEmpty()) {
            tag.put("Outputs", outputNbt);
        }
        tag.putBoolean("ignited", this.ignited);

        if (!this.tropicalFishData.isEmpty()) {
            ListTag list = new ListTag();
            for (CompoundTag fishTag : this.tropicalFishData) {
                list.add(fishTag.copy());
            }
            tag.put(TAG_TROPICAL_FISH_DATA, list);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public int getSignal() {
        return Math.round(15F * ((float) this.fluidHandler.getFluidAmount() / this.fluidHandler.getCapacity()));
    }

    // region 玩家交互
    public boolean tryInteractWithTank(Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) return false;
        if (this.level == null) return false;
        ItemStack inHand = player.getItemInHand(hand);
        if (this.interactWithFish(this.level, player, hand, inHand, hitResult)) return true;
        if (this.interactWithFluid(this.level, player, hand, inHand)) return true;
        return this.interactWithItems(this.level, player, hand, inHand, hitResult.getLocation());
    }

    // region 物品交互
    private boolean interactWithItems(Level level, Player player, InteractionHand hand, ItemStack inHand, Vec3 hitLoc) {
        return inHand.isEmpty()
               ? this.tryExtractAllItemsFromTank(level, player, hand)
               : this.tryInsertHandItemToTank(level, inHand, hitLoc);
    }

    private boolean tryExtractAllItemsFromTank(Level level, Player player, InteractionHand hand) {
        List<ItemStack> stacks = FishTankBlockEntity.extractAllFromTank(this.proxy, TriState.TRUE);
        if (stacks.isEmpty()) return false;
        if (level.isClientSide()) return true;

        Inventory inventory = player.getInventory();
        ItemStack first = stacks.getFirst();
        // 使误放入的工具类物品能回到手上
        if (first.getMaxStackSize() == 1) {
            player.setItemInHand(hand, first);
            for (int i = 1; i < stacks.size(); i++) {
                inventory.placeItemBackInInventory(stacks.get(i));
            }
            return true;
        }

        // 物品栏内没有相同物品，拿到手上
        int slot = inventory.getSlotWithRemainingSpace(first);
        if (slot == -1) {
            player.setItemInHand(hand, first);
            for (int i = 1; i < stacks.size(); i++) {
                inventory.placeItemBackInInventory(stacks.get(i));
            }
            return true;
        }

        for (ItemStack stack : stacks) {
            inventory.placeItemBackInInventory(stack);
        }
        return true;
    }

    private boolean tryInsertHandItemToTank(Level level, ItemStack inHand, Vec3 hitLoc) {
        if (!this.isValidInsertPos(hitLoc)) return false;
        if (inHand.is(ModItemTags.DISALLOW_HAND_INSERT_INTO_TANK)) return false;
        if (level.isClientSide()) return true;
        ItemStack remaining = FishTankBlockEntity.insertItemToTank(this.input, inHand.copy());
        int count = inHand.getCount();
        inHand.setCount(remaining.getCount());
        return remaining.getCount() != count;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isValidInsertPos(Vec3 hitLoc) {
        double x = hitLoc.x - this.getBlockPos().getX();
        double y = hitLoc.y - this.getBlockPos().getY();
        double z = hitLoc.z - this.getBlockPos().getZ();

        // 内外壁
        if (Math.abs(x - 0) < FishTankBlockEntity.EPSILON || Math.abs(x - 1) < FishTankBlockEntity.EPSILON) {
            return MathUtil.isInRange(y, 0.624, 1.001);
        }
        if (Math.abs(z - 0) < FishTankBlockEntity.EPSILON || Math.abs(z - 1) < FishTankBlockEntity.EPSILON) {
            return MathUtil.isInRange(y, 0.624, 1.001);
        }
        if (Math.abs(x - 0.0625) < FishTankBlockEntity.EPSILON || Math.abs(x - 0.9375) < FishTankBlockEntity.EPSILON) {
            return MathUtil.isInRange(z, y, 0.0624, 0.0624, 0.9376, 1.001);
        }
        if (Math.abs(y - 0.0625) < FishTankBlockEntity.EPSILON) {
            return MathUtil.isInRange(x, z, 0.0624, 0.0624, 0.9376, 0.9376);
        }
        if (Math.abs(z - 0.0625) < FishTankBlockEntity.EPSILON || Math.abs(z - 0.9375) < FishTankBlockEntity.EPSILON) {
            return MathUtil.isInRange(x, y, 0.0624, 0.0624, 0.9376, 1.001);
        }

        // 缸口
        if (Math.abs(x - 0.125) < FishTankBlockEntity.EPSILON || Math.abs(x - 0.875) < FishTankBlockEntity.EPSILON) {
            return MathUtil.isInRange(z, y, 0.124, 0.874, 0.876, 1.001);
        }
        if (Math.abs(y - 0.875) < FishTankBlockEntity.EPSILON) {
            return MathUtil.isInRange(x, z, 0.0624, 0.0624, 0.9376, 0.9376)
                   && !MathUtil.isInRange(x, z, 0.124, 0.124, 0.876, 0.876);
        }
        if (Math.abs(y - 1) < FishTankBlockEntity.EPSILON) {
            return !MathUtil.isInRange(x, z, 0.124, 0.124, 0.876, 0.876);
        }
        if (Math.abs(z - 0.125) < FishTankBlockEntity.EPSILON || Math.abs(z - 0.875) < FishTankBlockEntity.EPSILON) {
            return MathUtil.isInRange(x, y, 0.124, 0.874, 0.876, 1.001);
        }
        return false;
    }

    /**
     * 向鱼缸中放入物品
     *
     * @param handler 鱼缸物品处理器
     * @param entity  要放入的物品实体
     */
    public static void insertItemToTank(@Nullable IItemHandler handler, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        ItemStack remaining = FishTankBlockEntity.insertItemToTank(handler, stack.copy());
        if (remaining.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(remaining);
        }
    }

    /**
     * 向鱼缸中放入物品
     *
     * @param handler 鱼缸物品处理器
     * @param stack   要放入的物品
     * @return 剩余物品
     */
    public static ItemStack insertItemToTank(@Nullable IItemHandler handler, ItemStack stack) {
        if (handler == null) return stack;
        return ItemHandlerUtil.insertItem(handler, stack, false);
    }

    public void tryAutoOutputResults() {
        Level level = this.level;
        if (level == null || level.isClientSide()) return;
        BlockPos pos = this.getBlockPos();
        Direction outletDir = this.getBlockState().getValue(FishTankBlock.FACING);
        List<IItemHandler> targets = ItemHandlerUtil.getTargetItemHandlerList(pos.relative(outletDir), null, level);
        if (targets == null || targets.isEmpty()) {
            for (int i = 0; i < 8; i++) {
                ItemStack stack = this.output.extractItem(i, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) FishTankBlockEntity.popResourceFromFace(level, pos, outletDir, stack);
            }
            this.setChanged();
            this.refreshIgnited();
            this.sendUpdate();
            return;
        }
        for (IItemHandler target : targets) {
            for (int i = 0; i < 8; i++) {
                ItemStack extracted = this.output.extractItem(i, Integer.MAX_VALUE, true);
                if (extracted.isEmpty()) continue;
                ItemStack remaining = ItemHandlerUtil.insertItem(target, extracted, true);
                if (remaining.getCount() == extracted.getCount()) continue;
                remaining = ItemHandlerUtil.insertItem(
                    target,
                    this.output.extractItem(i, Integer.MAX_VALUE, false),
                    false
                );
                if (remaining.isEmpty()) continue;
                ItemHandlerUtil.insertItem(this.output, remaining, false);
            }
        }
        this.setChanged();
        this.refreshIgnited();
        this.sendUpdate();
    }

    /**
     * 从鱼缸中提取出所有物品
     *
     * @param handler            鱼缸物品处理器
     * @param containsIngredient 是否同时提取原料；<br>
     *                           {@link TriState#DEFAULT DEFAULT}为始终提取，<br>
     *                           {@link TriState#TRUE TRUE}为仅在产物为空时提取，<br>
     *                           {@link TriState#FALSE FALSE}为不提取
     * @return 提取出的所有物品
     */
    public static @Unmodifiable List<ItemStack> extractAllFromTank(IItemHandler handler, TriState containsIngredient) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ItemStack extracted = handler.extractItem(i, Integer.MAX_VALUE, true);
            if (extracted.isEmpty()) continue;
            int count = extracted.getCount();
            int maxSize = extracted.getMaxStackSize();
            if (count < maxSize) {
                result.add(handler.extractItem(i, count, false));
                continue;
            }
            for (; count > 0; count -= maxSize) {
                result.add(handler.extractItem(i, Math.min(count, maxSize), false));
            }
        }
        if (!containsIngredient.isFalse() && (containsIngredient.isDefault() || result.isEmpty())) {
            for (int i = 8; i < 16; i++) {
                ItemStack extracted = handler.extractItem(i, Integer.MAX_VALUE, true);
                if (extracted.isEmpty()) continue;
                int count = extracted.getCount();
                int maxSize = extracted.getMaxStackSize();
                if (count < maxSize) {
                    result.add(handler.extractItem(i, count, false));
                    continue;
                }
                for (; count > 0; count -= maxSize) {
                    result.add(handler.extractItem(i, Math.min(count, maxSize), false));
                }
            }
        }
        return ImmutableList.copyOf(result);
    }

    private static void popResourceFromFace(Level level, BlockPos pos, Direction direction, ItemStack stack) {
        int stepX = direction.getStepX();
        int stepY = direction.getStepY();
        int stepZ = direction.getStepZ();
        double halfWidth = (double) EntityType.ITEM.getWidth() / 2.0;
        double posX = (double) pos.getX() + 0.5
                      + (stepX == 0 ? Mth.nextDouble(level.random, -0.25, 0.25) : (double) stepX * (0.5 + halfWidth));
        double posY = pos.getY() + 0.5;
        double posZ = (double) pos.getZ() + 0.5
                      + (stepZ == 0 ? Mth.nextDouble(level.random, -0.25, 0.25) : (double) stepZ * (0.5 + halfWidth));
        double deltaX = stepX == 0 ? Mth.nextDouble(level.random, -0.1, 0.1) : (double) stepX * 0.1;
        double deltaY = stepY == 0 ? Mth.nextDouble(level.random, 0.0, 0.1) : (double) stepY * 0.1 + 0.1;
        double deltaZ = stepZ == 0 ? Mth.nextDouble(level.random, -0.1, 0.1) : (double) stepZ * 0.1;
        ItemEntity entity = new ItemEntity(level, posX, posY, posZ, stack, deltaX, deltaY, deltaZ);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
    // endregion

    // region 流体交互
    private boolean interactWithFluid(Level level, Player player, InteractionHand hand, ItemStack inHand) {
        if (FluidUtil.interactWithFluidHandler(player, hand, this.fluidHandler)) return true;
        if (inHand.is(Items.GLASS_BOTTLE)) {
            return this.tryFillEmptyBottle(level, player, hand, inHand);
        }
        return this.tryDrainFilledBottle(level, player, hand, inHand);
    }

    private boolean tryFillEmptyBottle(Level level, Player player, InteractionHand hand, ItemStack inHand) {
        FluidStack stack = this.fluidHandler.getFluid();
        BlockPos pos = this.getBlockPos();
        if (stack.is(Fluids.WATER)) {
            ItemStack result = Items.POTION.getDefaultInstance();
            player.setItemInHand(hand, ItemUtils.createFilledResult(inHand, player, result));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));

            FluidStack drained = this.fluidHandler.drain(250, IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() != 250) return false;
            if (level.isClientSide()) return true;
            this.fluidHandler.drain(250, IFluidHandler.FluidAction.EXECUTE);

            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            return true;
        } else if (stack.is(ModFluids.EXP_FLUID)) {
            ItemStack result = Items.EXPERIENCE_BOTTLE.getDefaultInstance();
            player.setItemInHand(hand, ItemUtils.createFilledResult(inHand, player, result));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));

            FluidStack drained = this.fluidHandler.drain(250, IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() != 250) return false;
            if (level.isClientSide()) return true;
            this.fluidHandler.drain(250, IFluidHandler.FluidAction.EXECUTE);

            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            return true;
        }
        return false;
    }

    private boolean tryDrainFilledBottle(Level level, Player player, InteractionHand hand, ItemStack inHand) {
        if (inHand.has(DataComponents.POTION_CONTENTS)) {
            PotionContents contents = inHand.get(DataComponents.POTION_CONTENTS);
            if (Objects.requireNonNull(contents).potion().isEmpty()) return false;
            Holder<Potion> potion = contents.potion().get();
            if (potion == Potions.WATER) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(inHand, player, Items.GLASS_BOTTLE.getDefaultInstance()));
                player.awardStat(Stats.FILL_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));

                FluidStack stack = new FluidStack(Fluids.WATER, 250);
                int filled = this.fluidHandler.fill(stack, IFluidHandler.FluidAction.SIMULATE);
                if (filled != 250) return false;
                if (level.isClientSide()) return true;
                this.fluidHandler.fill(stack, IFluidHandler.FluidAction.EXECUTE);

                BlockPos pos = this.getBlockPos();
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                return true;
            }
        } else if (inHand.is(Items.EXPERIENCE_BOTTLE)) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(inHand, player, Items.GLASS_BOTTLE.getDefaultInstance()));
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));

            // 50%概率
            if (level.getRandom().nextBoolean()) {
                FluidStack stack = new FluidStack(ModFluids.EXP_FLUID, 250);
                int filled = this.fluidHandler.fill(stack, IFluidHandler.FluidAction.SIMULATE);
                if (filled != 250) return false;
                if (level.isClientSide()) return true;
                this.fluidHandler.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            }

            BlockPos pos = this.getBlockPos();
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            return true;
        }
        return false;
    }

    public void entityInsideFluidContent(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) return;
        if (!this.fluidContentArea.intersects(entity.getBoundingBox())) return;

        FluidStack stack = this.fluidHandler.getFluid();
        if (this.isIgnited()) {
            if (!entity.fireImmune()) {
                entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 1);
                if (entity.getRemainingFireTicks() == 0) {
                    entity.igniteForSeconds(8.0F);
                }
            }
            entity.hurt(level.damageSources().inFire(), 4.0F);
        } else if (stack.is(Fluids.LAVA)) {
            entity.lavaHurt();
        } else if (entity.canFluidExtinguish(stack.getFluidType()) && entity.isOnFire()) {
            entity.clearFire();
            if (entity.mayInteract(level, pos)) {
                this.fluidHandler.drain(250, IFluidHandler.FluidAction.EXECUTE);
            }
            if (stack.is(ModFluids.POWDER_SNOW)) {
                this.fluidHandler.setFluid(new FluidStack(ModFluids.POWDER_SNOW, this.fluidHandler.getFluidAmount()));
            }
        } else if (stack.is(ModFluids.EXP_FLUID) && this.fluidHandler.getFluidAmount() == this.fluidHandler.getCapacity()) {
            if (!(entity instanceof Player player)) return;
            int capacity = this.fluidHandler.getCapacity();
            FluidStack drained = this.fluidHandler.drain(capacity, IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() != capacity) return;
            player.giveExperiencePoints(ExpFluidBlock.XP_POINTS);
            this.fluidHandler.drain(capacity, IFluidHandler.FluidAction.EXECUTE);
        }
    }
    // endregion

    // region 热带鱼交互
    public boolean interactWithFish(Level level, Player player, InteractionHand hand, ItemStack inHand, BlockHitResult hitResult) {
        if (hitResult.getLocation().y - hitResult.getBlockPos().getY() > 5 / 8F) return false;
        if (inHand.is(Items.TROPICAL_FISH_BUCKET)) {
            if (!this.fluidHandler.isEmpty() && !this.fluidHandler.getFluid().is(Fluids.WATER)) return false;
            if (this.isFullOfFish()) return false;
            if (level.isClientSide()) return true;

            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));
            this.tropicalFishData.add(FishTankBlockEntity.bucket2fishData(inHand));
            level.playSound(player, this.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, Items.WATER_BUCKET.getDefaultInstance());
            FluidUtil.interactWithFluidHandler(player, hand, this.fluidHandler);
            if (player.hasInfiniteMaterials()) {
                player.setItemInHand(hand, inHand);
            }
            this.updateFishState();
            return true;
        } else if (inHand.is(Items.WATER_BUCKET)) {
            if (this.isEmptyOfFish()) return false;
            if (level.isClientSide()) return true;

            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));
            CompoundTag fishData = this.tropicalFishData.removeLast();
            level.playSound(player, this.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, FishTankBlockEntity.fishData2bucket(fishData));
            this.updateFishState();
            return true;
        } else if (inHand.is(Items.BUCKET)) {
            if (this.isEmptyOfFish()) return false;
            if (this.fluidHandler.getFluidAmount() < this.fluidHandler.getCapacity()) return false;
            if (level.isClientSide()) return true;

            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));
            CompoundTag fishData = this.tropicalFishData.removeLast();
            FluidUtil.interactWithFluidHandler(player, hand, this.fluidHandler);
            level.playSound(player, this.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, FishTankBlockEntity.fishData2bucket(fishData));
            return true;
        }
        return false;
    }

    public void updateFishState() {
        if (this.level == null) return;

        if (this.isEmptyOfFish() && getBlockState().getValue(FishTankBlock.TROPICAL)) {
            this.level.setBlock(getBlockPos(), getBlockState().setValue(FishTankBlock.TROPICAL, false), 3);
        } else if (!this.isEmptyOfFish() && !getBlockState().getValue(FishTankBlock.TROPICAL)) {
            this.level.setBlock(getBlockPos(), getBlockState().setValue(FishTankBlock.TROPICAL, true), 3);
        }
    }

    public void dropFish() {
        if (level == null) return;
        for (CompoundTag data : tropicalFishData) {
            ItemStack bucket = FishTankBlockEntity.fishData2bucket(data);
            if (bucket.getItem() instanceof MobBucketItem mobBucket) {
                mobBucket.checkExtraContent(null, level, bucket, getBlockPos());
            }
        }
        tropicalFishData.clear();
    }

    public static CompoundTag bucket2fishData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        if (data == null) return FishTankBlockEntity.createTropicalFishData(0);
        CompoundTag tag = data.copyTag();
        return tag.copy();
    }

    public static ItemStack fishData2bucket(CompoundTag fishData) {
        ItemStack stack = new ItemStack(Items.TROPICAL_FISH_BUCKET);
        stack.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(fishData.copy()));
        return stack;
    }

    public static CompoundTag createTropicalFishData(int variant) {
        CompoundTag entityData = new CompoundTag();
        entityData.putString("id", "minecraft:tropical_fish");
        entityData.putInt("BucketVariantTag", variant);
        return entityData;
    }

    public boolean isFullOfFish() {
        return this.tropicalFishData.size() >= MAX_TROPICAL_FISH;
    }

    public boolean isEmptyOfFish() {
        return this.tropicalFishData.isEmpty();
    }
    // endregion

    // region 燃烧相关
    public static boolean canIgnite(FluidStack cur) {
        return cur.is(ModFluidTags.IGNITABLE);
    }

    public void setIgnited(boolean ignited) {
        if (this.ignited == ignited) return;
        this.ignited = ignited;
        this.setChanged();
        this.sendUpdate();
    }

    public void refreshIgnited() {
        if (!FishTankBlockEntity.canIgnite(this.fluidHandler.getFluid())) this.setIgnited(false);
        if (this.isIgnited()) return;
        for (int i = 0; i < this.proxy.getSlots(); i++) {
            ItemStack stack = this.proxy.getStackInSlot(i);
            if (stack.is(ModItemTags.FIRE_STARTER)) {
                stack.shrink(1);
                this.setIgnited(true);
            } else if (stack.is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
                this.setIgnited(true);
            }
        }
    }
    // endregion
    // endregion
}
