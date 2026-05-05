package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.PollableItemHandler;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

@Getter
public class FishTankBlockEntity extends BlockEntity implements IItemHandlerHolder, IFluidHandlerHolder {
    public static final int CAPACITY = FluidType.BUCKET_VOLUME;
    private final FluidTank fluidHandler = new FluidTank(CAPACITY);
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
    };

    public FishTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.fluidHandler.readFromNBT(provider, tag.getCompound("Fluid"));
        this.itemHandler.deserializeNBT(provider, tag.getCompound("Items"));
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
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean onPlayerUse(Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (FluidUtil.interactWithFluidHandler(player, hand, this.fluidHandler)) return true;
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
     * @param stack 要放入的物品
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
     * @param handler 鱼缸物品处理器
     * @param containsIngredient 是否同时提取原料；<br>
     *        {@link TriState#DEFAULT DEFAULT}为始终提取，<br>
     *        {@link TriState#TRUE TRUE}为仅在产物为空时提取，<br>
     *        {@link TriState#FALSE FALSE}为不提取
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
}
