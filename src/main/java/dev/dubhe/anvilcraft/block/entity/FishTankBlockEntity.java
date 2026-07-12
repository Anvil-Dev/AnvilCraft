package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.ItemResourceHandlerCache;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.fluid.FluidStackResourceHandler;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.PollableItemHandler;
import dev.dubhe.anvilcraft.block.fluid.ExpFluidBlock;
import dev.dubhe.anvilcraft.block.workstation.FishTankBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.mixin.accessor.StacksResourceHandlerAccessor;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

@Getter
public class FishTankBlockEntity extends BlockEntity implements IItemResourceHandlerHolder, ItemResourceHandlerCache,
    IFluidResourceHandlerHolder {
    private static final double EPSILON = 1.0 / 1024.0;
    public static final int MAX_TROPICAL_FISH = 4;

    private static final Vec3 FLUID_CONTENT_AREA_MIN = new Vec3(0.0625, 0.0625, 0.0625);
    private static final Vec3 FLUID_CONTENT_AREA_MAX = new Vec3(0.9375, 0.9375, 0.9375);
    private static final double FLUID_CONTENT_AREA_HEIGHT = 7.0 / 8;
    private static final String TAG_TROPICAL_FISH_DATA = "TropicalFishData";

    private final List<TropicalFishData> fishes = new ArrayList<>() {
        @Override
        public boolean add(TropicalFishData tag) {
            if (this.size() >= FishTankBlockEntity.MAX_TROPICAL_FISH) return false;
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.sendUpdate();
            return super.add(tag);
        }

        @Override
        public TropicalFishData removeLast() {
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.sendUpdate();
            return super.removeLast();
        }

        @Override
        public void clear() {
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.sendUpdate();
            super.clear();
        }
    };
    private AABB fluidContentArea = new AABB(FLUID_CONTENT_AREA_MIN, FLUID_CONTENT_AREA_MAX);
    private final FluidStackResourceHandler fluidHandler = new FluidStackResourceHandler() {
        @Override
        protected void onContentChanged(FluidStack original) {
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.refreshIgnited();
            FishTankBlockEntity.this.sendUpdate();
            FishTankBlockEntity.this.sendNeighbourUpdate();
            FishTankBlockEntity.this.updateLightLevel();
            this.updateContentArea();

            if (this.getResource(0).is(Fluids.WATER)) return;
            FishTankBlockEntity.this.dropAllFishes();
            FishTankBlockEntity.this.updateFishState();
        }

        private void updateContentArea() {
            double diffY = FishTankBlockEntity.FLUID_CONTENT_AREA_HEIGHT * (1.0 - (double) this.getFill());
            Vec3 pos = getBlockPos().getBottomCenter().subtract(0.5, 0, 0.5);
            FishTankBlockEntity.this.fluidContentArea = new AABB(
                FishTankBlockEntity.FLUID_CONTENT_AREA_MIN.add(pos),
                FishTankBlockEntity.FLUID_CONTENT_AREA_MAX.subtract(0, diffY, 0).add(pos)
            );
        }
    };

    /// 0-7 为输出产物，<br>
    /// 8-15 为输入原料
    private final ItemStacksResourceHandler proxy = new ItemStacksResourceHandler(16) {
        @Override
        public ItemResource getResource(int index) {
            if (index < 8) {
                return FishTankBlockEntity.this.output.getResource(index);
            } else {
                return FishTankBlockEntity.this.input.getResource(index - 8);
            }
        }

        @Override
        public long getAmountAsLong(int index) {
            if (index < 8) {
                return FishTankBlockEntity.this.output.getAmountAsLong(index);
            } else {
                return FishTankBlockEntity.this.input.getAmountAsLong(index - 8);
            }
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            if (index < 8) {
                return FishTankBlockEntity.this.output.getCapacityAsLong(index, resource);
            } else {
                return FishTankBlockEntity.this.input.getCapacityAsLong(index - 8, resource);
            }
        }

        @Override
        protected void setStacks(NonNullList<ItemStack> stacks) {
        }

        @Override
        public void set(int index, ItemResource resource, int amount) {
            if (index < 8) {
                FishTankBlockEntity.this.output.set(index, resource, amount);
            } else {
                FishTankBlockEntity.this.input.set(index - 8, resource, amount);
            }
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return index >= 8 && FishTankBlockEntity.this.input.isValid(index - 8, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            Objects.checkIndex(index, this.size());
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

            int currentAmount = this.getAmountAsInt(index);

            if ((currentAmount == 0 || resource.equals(this.getResource(index))) && this.isValid(index, resource)) {
                int inserted = Math.min(amount, this.getCapacityAsInt(index, resource) - currentAmount);

                if (inserted > 0) {
                    Util.<StacksResourceHandlerAccessor>cast(this).getSnapshotJournals().get(index).updateSnapshots(transaction);
                    this.set(index, resource, currentAmount + inserted);
                    return inserted;
                }
            }

            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            Objects.checkIndex(index, size());
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

            if (resource.equals(this.getResource(index))) {
                int currentAmount = this.getAmountAsInt(index);
                int extracted = Math.min(amount, currentAmount);

                if (extracted > 0) {
                    Util.<StacksResourceHandlerAccessor>cast(this).getSnapshotJournals().get(index).updateSnapshots(transaction);
                    this.set(index, resource, currentAmount - extracted);
                    return extracted;
                }
            }

            return 0;
        }
    };
    private final PollableItemHandler input = new PollableItemHandler(8) {
        @Override
        protected int getEmptyOrSmallerSlot(ItemResource resource) {
            int slot = -1;
            int countInSlot = Integer.MAX_VALUE;
            for (int i = 7; i >= 0; i--) {
                ItemResource resourceIn = this.getResource(i);
                if (resourceIn.isEmpty()) {
                    slot = i;
                    continue;
                }
                if (!resourceIn.equals(resource)) continue;
                if (countInSlot != Integer.MAX_VALUE) return -1;
                int amountIn = this.getAmountAsInt(i);
                if (amountIn < this.getCapacityAsIntDirect(i, resourceIn)) {
                    slot = i;
                    countInSlot = amountIn;
                } else {
                    return -1;
                }
            }
            return slot;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.refreshIgnited();
            FishTankBlockEntity.this.sendUpdate();
        }
    };
    private final ItemStacksResourceHandler output = new ItemStacksResourceHandler(8) {
        private boolean autoOutputting = false;

        @Override
        public boolean isValid(int index, ItemResource resource) {
            boolean hasSame = false;
            int sameIndex = -1;
            for (int i = 0; i < this.size(); i++) {
                if (!this.getResource(i).equals(resource)) {
                    continue;
                }

                if (hasSame) {
                    return false;
                } else {
                    hasSame = true;
                    sameIndex = i;
                }
            }
            if (!hasSame) return this.getResource(index).isEmpty();
            return sameIndex == index;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            ItemResource resourceIn = this.getResource(index);
            int maxSize = resourceIn.getMaxStackSize();
            if (resourceIn.isEmpty()) {
                maxSize = Item.DEFAULT_MAX_STACK_SIZE;
            }
            return Math.min(super.getCapacityAsLong(index, resource), maxSize);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            if (this.autoOutputting) return;
            this.checkAutoOutput(index);
            FishTankBlockEntity.this.setChanged();
            FishTankBlockEntity.this.refreshIgnited();
            FishTankBlockEntity.this.sendUpdate();
        }

        void checkAutoOutput(int index) {
            Level level = FishTankBlockEntity.this.level;
            if (level == null || level.isClientSide()) return;
            BlockState state = FishTankBlockEntity.this.getBlockState();
            if (!state.getValue(FishTankBlock.OUTLET)) return;
            try (Transaction transaction = Transaction.openRoot()) {
                ItemResource resourceIn = this.getResource(index);
                if (resourceIn.isEmpty()) return;
                int extracted = this.extract(index, resourceIn, Integer.MAX_VALUE, transaction);
                if (extracted <= 0) return;
                Direction outletDir = state.getValue(FishTankBlock.FACING);

                BlockPos pos = FishTankBlockEntity.this.getBlockPos();
                List<ResourceHandler<ItemResource>> targets = ItemHandlerUtil.getTargetItemHandlerList(
                    pos.relative(outletDir),
                    null,
                    level
                );
                if (targets == null || targets.isEmpty()) {
                    FishTankBlockEntity.popResourceFromFace(level, pos, outletDir, resourceIn.toStack(extracted));
                    transaction.commit();
                    return;
                }
                int remaining = extracted;
                for (ResourceHandler<ItemResource> target : targets) {
                    ItemStack remainingCache = resourceIn.toStack(remaining);
                    if (ItemHandlerUtil.insertItem(target, remainingCache, true).getCount() <= 0) continue;
                    remaining -= ItemHandlerUtil.insertItem(target, remainingCache, false).getCount();
                    if (remaining == 0) break;
                }
                this.autoOutputting = true;
                this.set(index, resourceIn, remaining);
                this.autoOutputting = false;
            }
        }
    };
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
            Block.UPDATE_CLIENTS
        );
    }

    private void sendNeighbourUpdate() {
        if (this.level == null) return;
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
    }

    private void updateLightLevel() {
        if (this.level == null) {
            return;
        }
        if (this.ignited) {
            return;
        }

        BlockPos pos = this.getBlockPos();
        AuxiliaryLightManager manager = this.level.getAuxLightManager(pos);
        if (manager == null) {
            return;
        }
        manager.setLightAt(pos, this.computeLightLevel());
    }

    private int computeLightLevel() {
        FluidStack stack = this.fluidHandler.getStack();
        return (int) Math.ceil(stack.getFluidType().getLightLevel(stack) * this.fluidHandler.getFill());
    }

    @Override
    public ItemStacksResourceHandler getItemHandler() {
        return this.proxy;
    }

    // region 持久化
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.fluidHandler.serialize(output.child("Fluid"));
        this.input.serialize(output.child("Inputs"));
        this.output.serialize(output.child("Outputs"));
        output.putBoolean("ignited", this.ignited);

        ValueOutput.TypedOutputList<TropicalFishData> list = output.list(TAG_TROPICAL_FISH_DATA, TropicalFishData.CODEC.codec());
        for (TropicalFishData fishTag : this.fishes) {
            list.add(fishTag);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.fluidHandler.deserialize(input.childOrEmpty("Fluid"));
        this.input.deserialize(input.childOrEmpty("Inputs"));
        this.output.deserialize(input.childOrEmpty("Outputs"));
        this.ignited = input.getBooleanOr("ignited", false);

        this.fishes.clear();
        for (TropicalFishData fishTag : input.listOrEmpty(TAG_TROPICAL_FISH_DATA, TropicalFishData.CODEC.codec())) {
            this.fishes.add(fishTag);
        }
    }
    // endregion

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), registries);

        this.fluidHandler.serialize(output.child("Fluid"));
        this.input.serialize(output.child("Inputs"));
        this.output.serialize(output.child("Outputs"));
        output.putBoolean("ignited", this.ignited);

        ValueOutput.TypedOutputList<TropicalFishData> list = output.list(TAG_TROPICAL_FISH_DATA, TropicalFishData.CODEC.codec());
        for (TropicalFishData fishTag : this.fishes) {
            list.add(fishTag);
        }

        return output.buildResult();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public int getSignal() {
        return Math.round(15F * this.fluidHandler.getFill());
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
        ItemStack inserted = FishTankBlockEntity.insertItemToTank(this.input, inHand.copy());
        int count = inHand.getCount();
        inHand.setCount(count - inserted.getCount());
        return inserted.getCount() != count;
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

    /// 向鱼缸中放入物品
    ///
    /// @param handler 鱼缸物品处理器
    /// @param entity  要放入的物品实体
    public static void insertItemToTank(@Nullable ResourceHandler<ItemResource> handler, ItemEntity entity) {
        if (!entity.anvilcraft$isAdsorbable()) {
            return;
        }
        ItemStack stack = entity.getItem();
        ItemStack inserted = FishTankBlockEntity.insertItemToTank(handler, stack.copy());
        if (inserted.getCount() == stack.getCount()) {
            entity.discard();
        } else {
            entity.setItem(inserted);
        }
    }

    /// 向鱼缸中放入物品
    ///
    /// @param handler 鱼缸物品处理器
    /// @param stack   要放入的物品
    ///
    /// @return 插入的物品
    public static ItemStack insertItemToTank(@Nullable ResourceHandler<ItemResource> handler, ItemStack stack) {
        if (handler == null) {
            return stack;
        }
        return ItemHandlerUtil.insertItem(handler, stack, false);
    }

    public void tryAutoOutputResults() {
        Level level = this.level;
        if (level == null || level.isClientSide()) return;
        BlockPos pos = this.getBlockPos();
        Direction outletDir = this.getBlockState().getValue(FishTankBlock.FACING);
        List<ResourceHandler<ItemResource>> targets = ItemHandlerUtil.getTargetItemHandlerList(pos.relative(outletDir), null, level);
        if (targets == null || targets.isEmpty()) {
            // 开口被有碰撞的方块堵住时不输出，物品留在输出槽等待下次重试
            if (isOutletBlocked(level, pos, outletDir)) return;
            for (int i = 0; i < 8; i++) {
                try (Transaction transaction = Transaction.openRoot()) {
                    ItemResource resource = this.output.getResource(i);
                    if (resource.isEmpty()) continue;
                    int extracted = this.output.extract(i, resource, Integer.MAX_VALUE, transaction);
                    if (extracted > 0) {
                        FishTankBlockEntity.popResourceFromFace(level, pos, outletDir, resource.toStack(extracted));
                    }
                    transaction.commit();
                }
            }
            this.setChanged();
            this.refreshIgnited();
            this.sendUpdate();
            return;
        }
        for (ResourceHandler<ItemResource> target : targets) {
            for (int i = 0; i < 8; i++) {
                try (Transaction transaction = Transaction.openRoot()) {
                    ItemResource resource = this.output.getResource(i);
                    if (resource.isEmpty()) continue;
                    int extracted = this.output.extract(i, resource, Integer.MAX_VALUE, transaction);
                    if (extracted <= 0) continue;
                    ItemStack inserted = ItemHandlerUtil.insertItem(target, resource.toStack(extracted), true);
                    if (inserted.isEmpty()) continue;
                    inserted = ItemHandlerUtil.insertItem(target, resource.toStack(extracted), false);
                    if (inserted.isEmpty()) continue;
                    this.output.insert(i, resource, extracted - inserted.getCount(), transaction);
                    transaction.commit();
                }
            }
        }
        this.setChanged();
        this.refreshIgnited();
        this.sendUpdate();
    }

    /**
     * 判断输出口开口处是否被前方方块的碰撞形状堵住。
     *
     * @param level     世界
     * @param pos       鱼缸坐标
     * @param direction 输出口朝向
     * @return 被堵返回 true
     */
    private static boolean isOutletBlocked(Level level, BlockPos pos, Direction direction) {
        // 开口中心紧贴鱼缸与前方方块的交界面
        Vec3 openingCenter = new Vec3(
            pos.getX() + 0.5 + direction.getStepX() * 0.5,
            pos.getY() + 0.5 + direction.getStepY() * 0.5,
            pos.getZ() + 0.5 + direction.getStepZ() * 0.5
        );
        return AnvilUtil.isOutletBlocked(level, pos.relative(direction), openingCenter, direction);
    }

    /// 从鱼缸中提取出所有物品
    ///
    /// @param handler            鱼缸物品处理器
    /// @param containsIngredient 是否同时提取原料；<br>
    ///                           {@link TriState#DEFAULT DEFAULT}为始终提取，<br>
    ///                           {@link TriState#TRUE TRUE}为仅在产物为空时提取，<br>
    ///                           {@link TriState#FALSE FALSE}为不提取
    ///
    /// @return 提取出的所有物品
    public static @Unmodifiable List<ItemStack> extractAllFromTank(ResourceHandler<ItemResource> handler, TriState containsIngredient) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            try (Transaction transaction = Transaction.openRoot()) {
                ItemResource resource = handler.getResource(i);
                if (resource.isEmpty()) continue;
                int extracted = handler.extract(i, resource, Integer.MAX_VALUE, transaction);
                if (extracted <= 0) continue;
                int maxSize = resource.getMaxStackSize();
                if (extracted < maxSize) {
                    result.add(resource.toStack(extracted));
                    transaction.commit();
                    continue;
                }
                for (; extracted > 0; extracted -= maxSize) {
                    result.add(resource.toStack(Math.min(extracted, maxSize)));
                }
                transaction.commit();
            }
        }
        if (!containsIngredient.isFalse() && (containsIngredient.isDefault() || result.isEmpty())) {
            for (int i = 8; i < 16; i++) {
                try (Transaction transaction = Transaction.openRoot()) {
                    ItemResource resource = handler.getResource(i);
                    if (resource.isEmpty()) continue;
                    int extracted = handler.extract(i, resource, Integer.MAX_VALUE, transaction);
                    if (extracted <= 0) continue;
                    int maxSize = resource.getMaxStackSize();
                    if (extracted < maxSize) {
                        result.add(resource.toStack(extracted));
                        transaction.commit();
                        continue;
                    }
                    for (; extracted > 0; extracted -= maxSize) {
                        result.add(resource.toStack(Math.min(extracted, maxSize)));
                    }
                    transaction.commit();
                }
            }
        }
        return ImmutableList.copyOf(result);
    }

    private static void popResourceFromFace(Level level, BlockPos pos, Direction direction, ItemStack stack) {
        int stepX = direction.getStepX();
        int stepY = direction.getStepY();
        int stepZ = direction.getStepZ();
        double extra = 0.5 + (double) EntityType.ITEM.getWidth() / 2.0;
        double posX = (double) pos.getX() + 0.5 + stepX * extra;
        double posY = (double) pos.getY() + 0.5 + stepY * extra;
        double posZ = (double) pos.getZ() + 0.5 + stepZ * extra;
        Vec3 delta = new Vec3(stepX, stepY, stepZ).multiply(0.25, 0.25, 0.25);
        ItemEntity entity = new ItemEntity(level, posX, posY, posZ, stack, delta.x, delta.y, delta.z);
        entity.anvilcraft$setIsAdsorbable(false);
        level.addFreshEntity(entity);
    }
    // endregion

    // region 流体交互
    public boolean interactWithFluid(Level level, Player player, InteractionHand hand, ItemStack inHand) {
        try (Transaction transaction = Transaction.openRoot()) {
            boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.fluidHandler, transaction);
            if (success) {
                transaction.commit();
                return true;
            }
        }
        if (inHand.is(Items.GLASS_BOTTLE)) {
            return FishTankBlockEntity.tryFillEmptyBottle(
                level,
                this.getBlockPos(),
                player,
                inHand,
                result -> player.setItemInHand(hand, ItemUtils.createFilledResult(inHand, player, result)),
                this.fluidHandler
            );
        }
        return FishTankBlockEntity.tryDrainFilledBottle(
            level,
            this.getBlockPos(),
            player,
            inHand,
            result -> player.setItemInHand(hand, ItemUtils.createFilledResult(inHand, player, result)),
            this.fluidHandler
        );
    }

    public static boolean tryFillEmptyBottle(
        Level level,
        BlockPos pos,
        @Nullable Player player,
        ItemStack bottle,
        Consumer<ItemStack> setter,
        FluidStackResourceHandler handler
    ) {
        ItemStack result = null;
        FluidStack stack = handler.getStack();
        if (stack.is(Fluids.WATER)) {
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = handler.extract(0, FluidResource.of(stack), 250, transaction);
                if (extracted != 250) return false;
                if (level.isClientSide()) return true;
                transaction.commit();
                result = Items.POTION.getDefaultInstance();
            }
        } else if (stack.is(ModFluids.HONEY)) {
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = handler.extract(0, FluidResource.of(stack), 250, transaction);
                if (extracted != 250) return false;
                if (level.isClientSide()) return true;
                transaction.commit();
                result = Items.HONEY_BOTTLE.getDefaultInstance();
            }
        } else if (stack.is(ModFluids.EXP_FLUID)) {
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = handler.extract(0, FluidResource.of(stack), 250, transaction);
                if (extracted != 250) return false;
                if (level.isClientSide()) return true;
                transaction.commit();
                result = Items.EXPERIENCE_BOTTLE.getDefaultInstance();
            }
        }

        if (result == null) {
            return false;
        }

        setter.accept(result);
        if (player != null) {
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(bottle.getItem()));
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS);
        level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        return true;
    }

    public static boolean tryDrainFilledBottle(
        Level level,
        BlockPos pos,
        @Nullable Player player,
        ItemStack bottle,
        Consumer<ItemStack> setter,
        FluidStackResourceHandler handler
    ) {
        boolean success = false;
        if (bottle.has(DataComponents.POTION_CONTENTS)) {
            PotionContents contents = bottle.get(DataComponents.POTION_CONTENTS);
            if (Objects.requireNonNull(contents).potion().isEmpty()) return false;
            Holder<Potion> potion = contents.potion().get();
            if (potion == Potions.WATER) {
                try (Transaction transaction = Transaction.openRoot()) {
                    int inserted = handler.insert(0, FluidResource.of(Fluids.WATER), 250, transaction);
                    if (inserted != 250) return false;
                    if (level.isClientSide()) return true;
                    transaction.commit();
                    success = true;
                }
            }
        } else if (bottle.is(Items.HONEY_BOTTLE)) {
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(0, FluidResource.of(ModFluids.HONEY), 250, transaction);
                if (inserted != 250) return false;
                if (level.isClientSide()) return true;
                transaction.commit();
                success = true;
            }
        } else if (bottle.is(Items.EXPERIENCE_BOTTLE)) {
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(0, FluidResource.of(ModFluids.EXP_FLUID), 250, transaction);
                if (inserted != 250) return false;
                if (level.isClientSide()) return true;
                // 50%概率
                if (level.getRandom().nextBoolean()) {
                    transaction.commit();
                    success = true;
                }
            }
        }

        if (!success) {
            return false;
        }

        setter.accept(Items.GLASS_BOTTLE.getDefaultInstance());
        if (player != null) {
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(bottle.getItem()));
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS);
        level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        return true;
    }

    public void entityInsideFluidContent(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier) {
        if (level.isClientSide()) return;
        ServerLevel serverside = Util.cast(level);
        if (!this.fluidContentArea.intersects(entity.getBoundingBox())) return;

        FluidStack stack = this.fluidHandler.getStack();
        if (this.isIgnited()) {
            effectApplier.apply(InsideBlockEffectType.FIRE_IGNITE);
        } else if (stack.is(Fluids.LAVA)) {
            effectApplier.apply(InsideBlockEffectType.LAVA_IGNITE);
        } else if (entity.canFluidExtinguish(stack.getFluidType()) && entity.isOnFire()) {
            effectApplier.apply(InsideBlockEffectType.EXTINGUISH);
            FluidResource resource = FluidResource.of(stack);
            try (Transaction transaction = Transaction.openRoot()) {
                if (entity.mayInteract(serverside, pos)) {
                    this.fluidHandler.extract(0, resource, 250, transaction);
                }
                if (stack.is(ModFluids.POWDER_SNOW)) {
                    this.fluidHandler.set(resource, this.fluidHandler.getAmountAsInt(0));
                }
                transaction.commit();
            }
        } else if (stack.is(ModFluids.EXP_FLUID) && this.fluidHandler.isFull()) {
            if (!(entity instanceof Player player)) return;
            FluidResource resource = FluidResource.of(stack);
            int capacity = this.fluidHandler.getCapacityAsInt(0, resource);
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = this.fluidHandler.extract(0, resource, capacity, transaction);
                if (extracted != capacity) return;
                player.giveExperiencePoints(ExpFluidBlock.XP_POINTS);
                transaction.commit();
            }
        }
    }
    // endregion

    // region 热带鱼交互
    public boolean interactWithFish(Level level, Player player, InteractionHand hand, ItemStack inHand, BlockHitResult hitResult) {
        if (hitResult.getLocation().y - hitResult.getBlockPos().getY() > 5 / 8F) return false;
        if (inHand.is(Items.TROPICAL_FISH_BUCKET)) {
            if (!this.fluidHandler.getStack().isEmpty() && !this.fluidHandler.getStack().is(Fluids.WATER)) return false;
            if (this.isFullOfFish()) return false;
            if (level.isClientSide()) return true;

            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));
            this.fishes.add(TropicalFishData.fromBucket(inHand));
            level.playSound(player, this.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, Items.WATER_BUCKET.getDefaultInstance());
            try (Transaction transaction = Transaction.openRoot()) {
                boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.fluidHandler, transaction);
                if (success) transaction.commit();
            }
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
            TropicalFishData fishData = this.fishes.removeLast();
            level.playSound(player, this.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, fishData.toBucket());
            this.updateFishState();
            return true;
        } else if (inHand.is(Items.BUCKET)) {
            if (this.isEmptyOfFish()) return false;
            if (!this.fluidHandler.isFull()) return false;
            if (level.isClientSide()) return true;

            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));
            TropicalFishData fishData = this.fishes.removeLast();
            try (Transaction transaction = Transaction.openRoot()) {
                boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.fluidHandler, transaction);
                if (success) transaction.commit();
            }
            level.playSound(player, this.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, fishData.toBucket());
            return true;
        }
        return false;
    }

    public void updateFishState() {
        if (this.level == null) return;

        if (this.isEmptyOfFish() && this.getBlockState().getValue(FishTankBlock.TROPICAL)) {
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(FishTankBlock.TROPICAL, false), 18);
        } else if (!this.isEmptyOfFish() && !getBlockState().getValue(FishTankBlock.TROPICAL)) {
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(FishTankBlock.TROPICAL, true), 18);
        }
    }

    public void dropAllFishes() {
        if (this.level == null) return;
        for (TropicalFishData data : this.fishes) {
            ItemStack bucket = data.toBucket();
            if (bucket.getItem() instanceof MobBucketItem mobBucket) {
                mobBucket.checkExtraContent(null, this.level, bucket, this.getBlockPos());
            }
        }
        this.fishes.clear();
    }

    public boolean isFullOfFish() {
        return this.fishes.size() >= MAX_TROPICAL_FISH;
    }

    public boolean isEmptyOfFish() {
        return this.fishes.isEmpty();
    }

    public record TropicalFishData(TropicalFish.Pattern pattern, DyeColor baseColor, DyeColor patternColor) {
        public static final MapCodec<TropicalFishData> CODEC = CodecUtil.mapCodec(
            TropicalFish.Pattern.CODEC
                .fieldOf("pattern")
                .forGetter(TropicalFishData::pattern),
            DyeColor.CODEC
                .fieldOf("base_color")
                .forGetter(TropicalFishData::baseColor),
            DyeColor.CODEC
                .fieldOf("pattern_color")
                .forGetter(TropicalFishData::patternColor),
            TropicalFishData::new
        );
        public static final StreamCodec<ByteBuf, TropicalFishData> STREAM_CODEC = StreamCodec.composite(
            TropicalFish.Pattern.STREAM_CODEC,
            TropicalFishData::pattern,
            DyeColor.STREAM_CODEC,
            TropicalFishData::baseColor,
            DyeColor.STREAM_CODEC,
            TropicalFishData::patternColor,
            TropicalFishData::new
        );

        public static TropicalFishData fromBucket(ItemStack bucket) {
            Random random = new Random(new Random().nextLong());
            TropicalFish.Pattern pattern = bucket.get(DataComponents.TROPICAL_FISH_PATTERN);
            if (pattern == null) {
                pattern = TropicalFish.Pattern.values()[random.nextInt(TropicalFish.Pattern.values().length)];
            }
            DyeColor baseColor = bucket.get(DataComponents.TROPICAL_FISH_BASE_COLOR);
            if (baseColor == null) {
                baseColor = DyeColor.values()[random.nextInt(DyeColor.values().length)];
            }
            DyeColor patternColor = bucket.get(DataComponents.TROPICAL_FISH_PATTERN_COLOR);
            if (patternColor == null) {
                patternColor = DyeColor.values()[random.nextInt(DyeColor.values().length)];
            }
            return new TropicalFishData(pattern, baseColor, patternColor);
        }

        public ItemStack toBucket() {
            ItemStack stack = new ItemStack(Items.TROPICAL_FISH_BUCKET);
            stack.set(DataComponents.TROPICAL_FISH_PATTERN, this.pattern);
            stack.set(DataComponents.TROPICAL_FISH_BASE_COLOR, this.baseColor);
            stack.set(DataComponents.TROPICAL_FISH_PATTERN_COLOR, this.patternColor);
            return stack;
        }
    }
    // endregion

    // region 燃烧相关
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canIgnite(FluidStack cur) {
        return cur.is(ModFluidTags.IGNITABLE);
    }

    public void setIgnited(boolean ignited) {
        if (this.ignited == ignited) {
            return;
        }
        this.ignited = ignited;
        this.setChanged();
        this.sendUpdate();

        if (this.level == null) {
            return;
        }
        BlockPos pos = this.getBlockPos();
        AuxiliaryLightManager manager = this.level.getAuxLightManager(pos);
        if (manager == null) {
            return;
        }
        manager.setLightAt(pos, ignited ? 15 : this.computeLightLevel());
    }

    public void refreshIgnited() {
        if (!FishTankBlockEntity.canIgnite(this.fluidHandler.getStack())) {
            this.setIgnited(false);
            return;
        }
        if (this.isIgnited()) {
            Level level = this.getLevel();
            if (level != null && this.isIgnited() && level.getBlockState(this.getBlockPos().below()).is(ModBlocks.HEATER)) {
                level.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
            }
            return;
        }
        for (int i = 0; i < this.proxy.size(); i++) {
            ItemResource resource = this.proxy.getResource(i);
            if (resource.is(ModItemTags.FIRE_STARTER)) {
                try (Transaction transaction = Transaction.openRoot()) {
                    this.proxy.extract(i, this.proxy.getResource(i), 1, transaction);
                    transaction.commit();
                }
                this.setIgnited(true);
            } else if (resource.is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
                this.setIgnited(true);
            }
        }
        Level level = this.getLevel();
        if (level != null && this.isIgnited() && level.getBlockState(this.getBlockPos().below()).is(ModBlocks.HEATER)) {
            level.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
        }
    }
    // endregion
    // endregion

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (SmartBlockPlacerBlockEntity.isBlockBeingMovedByPlacer()) return;
        ResourceHandler<ItemResource> handler = this.getItemHandler();
        for (int slot = 0; slot < handler.size(); slot++) {
            try (Transaction transaction = Transaction.openRoot()) {
                ItemResource resource = handler.getResource(slot);
                if (resource.isEmpty()) continue;
                int extracted = handler.extract(slot, resource, Integer.MAX_VALUE, transaction);
                if (extracted > 0) {
                    Block.popResource(Objects.requireNonNull(this.level), pos, resource.toStack(extracted));
                }
                transaction.commit();
            }
        }
        this.dropAllFishes();
    }
}
