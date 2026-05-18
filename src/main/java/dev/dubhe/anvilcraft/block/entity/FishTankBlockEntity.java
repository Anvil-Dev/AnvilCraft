package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.recipe.cache.IItemHandlerCache;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.PollableItemHandler;
import dev.dubhe.anvilcraft.block.FishTankBlock;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

@Getter
public class FishTankBlockEntity extends BlockEntity implements IItemHandlerHolder, IItemHandlerCache, IFluidHandlerHolder {
    public static final int CAPACITY = FluidType.BUCKET_VOLUME;
    public static final int MAX_TROPICAL_FISH = 4;
    public static final Double FISH_HEIGHT = 0.75D;

    private static final String TAG_TROPICAL_FISH_DATA = "TropicalFishData";

    private final List<CompoundTag> tropicalFishData = new ArrayList<>() {
        @Override
        public boolean add(CompoundTag tag) {
            if (this.size() >= MAX_TROPICAL_FISH) return false;
            setChanged();
            sendUpdate();
            return super.add(tag);
        }

        @Override
        public CompoundTag removeLast() {
            setChanged();
            sendUpdate();
            return super.removeLast();
        }

        @Override
        public void clear() {
            setChanged();
            sendUpdate();
            super.clear();
        }
    };
    private final FluidTank fluidHandler = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.refreshIgnited();
            sendUpdate();
            if (!isWaterArea(this)) {
                FishTankBlockEntity.this.dropFish();
                FishTankBlockEntity.this.updateFishState();
            }
        }
    };

    /**
     * 0-7 为输出产物，<br>
     * 8-15 为输入物品
     */
    private final PollableItemHandler itemHandler = new PollableItemHandler(16) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 8 && slot == this.getEmptyOrSmallerSlot(stack);
        }

        @Override
        protected int getEmptyOrSmallerSlot(ItemStack stack) {
            int slot = -1;
            int countInSlot = Integer.MAX_VALUE;
            for (int i = 15; i >= 8; i--) {
                ItemStack stackInSlot = this.getStackInSlot(i);
                if (!stackInSlot.isEmpty() && !ItemStack.isSameItemSameComponents(stackInSlot, stack)) continue;
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
            if (
                slot < 8
                && getBlockState().getValue(FishTankBlock.OUTLET)
                && !this.getStackInSlot(slot).isEmpty()
            ) {
                Direction outletDir = getBlockState().getValue(FishTankBlock.FACING);
                if (level != null) {
                    ItemStack stack = this.extractItem(slot, Integer.MAX_VALUE, false);
                    if (!stack.isEmpty()) FishTankBlockEntity.popResourceFromFace(level, getBlockPos(), outletDir, stack);
                }
            }
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.refreshIgnited();
            sendUpdate();
        }
    };
    private final PollableItemHandler outputProxy = new PollableItemHandler(8) {
        @Override
        public ItemStack getStackInSlot(int slot) {
            return FishTankBlockEntity.this.itemHandler.getStackInSlot(slot);
        }

        @Override
        public int getSlotLimit(int slot) {
            return FishTankBlockEntity.this.itemHandler.getSlotLimit(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            } else if (!this.isItemValid(slot, stack)) {
                return stack;
            } else {
                this.validateSlotIndex(slot);
                ItemStack existing = this.getStackInSlot(slot);
                int limit = this.getStackLimit(slot, stack);
                if (!existing.isEmpty()) {
                    if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                        return stack;
                    }

                    limit -= existing.getCount();
                }

                if (limit <= 0) {
                    return stack;
                } else {
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
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount == 0) return ItemStack.EMPTY;
            this.validateSlotIndex(slot);
            ItemStack existing = this.getStackInSlot(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            int toExtract = Math.min(amount, existing.getMaxStackSize());
            if (existing.getCount() <= toExtract) {
                if (simulate) return existing.copy();
                this.setStackInSlot(slot, ItemStack.EMPTY);
                this.onContentsChanged(slot);
                return existing;
            } else {
                if (simulate) return existing.copyWithCount(toExtract);
                this.setStackInSlot(slot, existing.copyWithCount(existing.getCount() - toExtract));
                this.onContentsChanged(slot);
                return existing.copyWithCount(toExtract);
            }
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            FishTankBlockEntity.this.itemHandler.setStackInSlot(slot, stack);
        }
    };
    private boolean ignited = false;

    public FishTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateFishState();
    }

    public void setIgnited(boolean ignited) {
        if (this.ignited == ignited) return;
        this.ignited = ignited;
        this.setChanged();
        sendUpdate();
    }

    private void sendUpdate() {
        if (level == null) return;
        level.sendBlockUpdated(
            getBlockPos(),
            getBlockState(),
            getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    @Override
    public PollableItemHandler getInput() {
        return this.itemHandler;
    }

    @Override
    public PollableItemHandler getOutput() {
        return this.outputProxy;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        CompoundTag tankNbt = this.fluidHandler.writeToNBT(provider, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("Fluid", tankNbt);
        }
        CompoundTag chestNbt = this.itemHandler.serializeNBT(provider);
        if (!chestNbt.isEmpty()) {
            tag.put("Items", chestNbt);
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
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.fluidHandler.readFromNBT(provider, tag.getCompound("Fluid"));
        this.itemHandler.deserializeNBT(provider, tag.getCompound("Items"));
        this.ignited = tag.getBoolean("ignited") && FishTankBlockEntity.canIgnite(this.fluidHandler.getFluid());

        this.tropicalFishData.clear();
        if (tag.contains(TAG_TROPICAL_FISH_DATA, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_TROPICAL_FISH_DATA, Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(MAX_TROPICAL_FISH, list.size()); i++) {
                this.tropicalFishData.add(list.getCompound(i).copy());
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag tankNbt = this.fluidHandler.writeToNBT(registries, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("Fluid", tankNbt);
        }
        CompoundTag chestNbt = this.itemHandler.serializeNBT(registries);
        if (!chestNbt.isEmpty()) {
            tag.put("Items", chestNbt);
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

    public boolean onPlayerUse(Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack inHand = player.getItemInHand(hand);
        if (isLowerSideArea(hitResult) && level != null) {
            if (interactWithFish(level, player, hand)) return true;
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, this.fluidHandler)) return true;
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

    public boolean interactWithFish(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.TROPICAL_FISH_BUCKET)) {
            if (!canPlaceFish(fluidHandler)) return false;
            if (this.isFullOfFish()) return false;

            this.tropicalFishData.add(FishTankBlockEntity.bucket2fishData(stack));
            level.playSound(player, this.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, Items.WATER_BUCKET.getDefaultInstance());
            FluidUtil.interactWithFluidHandler(player, hand, this.fluidHandler);
            updateFishState();
            return true;
        } else if (stack.is(Items.WATER_BUCKET)) {
            if (this.isEmptyOfFish()) return false;

            CompoundTag fishData = this.tropicalFishData.removeLast();
            level.playSound(player, this.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, FishTankBlockEntity.fishData2bucket(fishData));
            updateFishState();
            return true;
        } else if (stack.is(Items.BUCKET)) {
            if (this.isEmptyOfFish()) return false;
            if (fluidHandler.getFluidAmount() < fluidHandler.getCapacity()) return false;

            CompoundTag fishData = this.tropicalFishData.removeLast();
            FluidUtil.interactWithFluidHandler(player, hand, this.fluidHandler);
            level.playSound(player, this.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, FishTankBlockEntity.fishData2bucket(fishData));
            return true;
        }
        return false;
    }

    public static boolean isWaterArea(FluidTank fluidHandler) {
        return !fluidHandler.isEmpty() && fluidHandler.getFluid().is(Fluids.WATER);
    }

    public static boolean canPlaceFish(FluidTank fluidHandler) {
        return fluidHandler.isEmpty() || fluidHandler.getFluid().is(Fluids.WATER);
    }

    public static boolean isLowerSideArea(BlockHitResult hitResult) {
        Direction dir = hitResult.getDirection();
        if (!dir.getAxis().isHorizontal()) return false;
        double relY = hitResult.getLocation().y - hitResult.getBlockPos().getY();
        return relY < FISH_HEIGHT;
    }

    public void updateFishState() {
        if (level == null) return;

        if (this.isEmptyOfFish() && getBlockState().getValue(FishTankBlock.TROPICAL)) {
            level.setBlock(getBlockPos(), getBlockState().setValue(FishTankBlock.TROPICAL, false), 3);
        } else if (!this.isEmptyOfFish() && !getBlockState().getValue(FishTankBlock.TROPICAL)) {
            level.setBlock(getBlockPos(), getBlockState().setValue(FishTankBlock.TROPICAL, true), 3);
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

    /**
     * 向鱼缸中放入物品
     *
     * @param handler 鱼缸物品处理器
     * @param entity  要放入的物品实体
     */
    public static void insertToTank(@Nullable IItemHandler handler, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (entity.anvilcraft$isAdsorbable()) {
            FishTankBlockEntity.insertToTank(handler, stack);
            return;
        }
        if (!(handler instanceof IItemHandlerModifiable modifiable)) return;
        int remaining = stack.getCount();
        while (remaining > 0) {
            int slot = -1;
            for (int i = 0; i < 8; i++) {
                ItemStack stackInSlot = handler.getStackInSlot(i);
                if (!stackInSlot.isEmpty() && !ItemStack.isSameItemSameComponents(stackInSlot, stack)) continue;
                int limit = Math.min(stackInSlot.getMaxStackSize(), handler.getSlotLimit(i));
                if (stackInSlot.getCount() >= limit) continue;
                slot = i;
                break;
            }
            if (slot < 0) return;
            ItemStack stackInSlot = handler.getStackInSlot(slot);
            int limit = Math.min(
                stackInSlot.isEmpty() ? Item.DEFAULT_MAX_STACK_SIZE : stackInSlot.getMaxStackSize(),
                handler.getSlotLimit(slot)
            );
            int existing = stackInSlot.getCount();
            int storing = Math.min(remaining, limit - existing);
            remaining -= storing;
            modifiable.setStackInSlot(slot, stack.copyWithCount(existing + storing));
        }
        entity.discard();
    }

    /**
     * 向鱼缸中放入物品
     *
     * @param handler 鱼缸物品处理器
     * @param stack   要放入的物品
     * @return 是否放入成功
     */
    public static boolean insertToTank(@Nullable IItemHandler handler, ItemStack stack) {
        if (handler == null) return false;
        if (stack.is(ModItemTags.DISALLOW_HAND_INSERT_INTO_TANK)) return false;
        for (int i = 8; i < 16; i++) {
            ItemStack inserted = handler.insertItem(i, stack.copy(), true);
            int diff = stack.getCount() - inserted.getCount();
            if (diff == 0) continue;
            handler.insertItem(i, stack.split(diff), false);
            return true;
        }
        return false;
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

    public static boolean canIgnite(FluidStack cur) {
        return cur.is(ModFluidTags.IGNITABLE);
    }

    public void refreshIgnited() {
        if (!FishTankBlockEntity.canIgnite(this.fluidHandler.getFluid())) this.setIgnited(false);
        if (!this.isIgnited()) {
            for (int i = 0; i < this.itemHandler.getSlots(); i++) {
                ItemStack stack = this.itemHandler.getStackInSlot(i);
                if (stack.is(ModItemTags.FIRE_STARTER)) {
                    stack.shrink(1);
                    this.setIgnited(true);
                } else if (stack.is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
                    this.setIgnited(true);
                }
            }
        }
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
}
