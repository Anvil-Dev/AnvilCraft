package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * 控制阀的 BlockEntity。存储流体过滤（白名单）和最大流速，供流体网络分配时门控。
 *
 * <h3>过滤</h3>
 * 白名单直接以 {@link FluidStack} 存储（<b>不经桶物品</b>，因为部分流体如蜂蜜、原始物质无对应桶），
 * 由玩家在 GUI 中手动放入桶 / 从 JEI 拖入流体设置。未设置任何过滤 → 允许所有流体通过。
 *
 * <h3>流速</h3>
 * {@link #maxRate} ∈ [0, {@value #MAX_RATE}] mB/tick，只限制每 tick 流过本阀门的流体上限，
 * 不是最低起送量；白名单只放行已标记的流体，不会用其他液体补足流速。
 */
@Getter
public class ControlValveBlockEntity extends BlockEntity implements MenuProvider {
    /** 过滤槽数量 */
    public static final int FILTER_SLOT_COUNT = 1;
    /** 最大可设流速（mB/tick） */
    public static final int MAX_RATE = 2000;

    /** 白名单过滤流体（每个"槽"一种；{@link FluidStack#EMPTY} 表示该槽未设置） */
    private final NonNullList<FluidStack> filters = NonNullList.withSize(FILTER_SLOT_COUNT, FluidStack.EMPTY);

    /** 允许通过的最大流速（mB/tick） */
    private int maxRate = MAX_RATE;

    /**
     * 手轮朝向的那一面（放置时记录玩家面向的、垂直于轴的方向）。手轮 BER 渲染在此面中心、绕此面法线旋转。
     * 仅放置时确定一次，不随玩家移动更新。默认 NORTH（放置逻辑会覆写）。
     */
    private Direction facing = Direction.NORTH;

    public ControlValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
        this.setChanged();
    }

    public void setMaxRate(int maxRate) {
        this.maxRate = Mth.clamp(maxRate, 0, MAX_RATE);
        this.setChanged();
    }

    /** 是否被红石锁定（读方块状态 POWERED）。锁定时有效流速为 0 且 GUI 不可调。 */
    public boolean isLocked() {
        BlockState state = getBlockState();
        return state.hasProperty(ControlValveBlock.POWERED) && state.getValue(ControlValveBlock.POWERED);
    }

    /** 供网络分配使用的有效最大流速：被红石锁定时为 0，否则为设定值。 */
    public int getEffectiveMaxRate() {
        return isLocked() ? 0 : maxRate;
    }

    /** 设置某槽的过滤流体（数量归一，仅记录种类）。 */
    public void setFilter(int index, FluidStack fluid) {
        if (index < 0 || index >= filters.size()) {
            return;
        }
        filters.set(index, fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1));
        this.setChanged();
    }

    public FluidStack getFilter(int index) {
        return (index < 0 || index >= filters.size()) ? FluidStack.EMPTY : filters.get(index);
    }

    /**
     * 判断某流体是否被允许通过本阀门。
     *
     * @return 白名单为空 → 全部允许；否则仅白名单内的流体允许
     */
    public boolean allows(FluidStack fluid) {
        boolean anySet = false;
        for (FluidStack allowed : filters) {
            if (allowed.isEmpty()) {
                continue;
            }
            anySet = true;
            if (FluidStack.isSameFluidSameComponents(allowed, fluid)) {
                return true;
            }
        }
        return !anySet;
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MaxRate", maxRate);
        tag.putInt("Facing", facing.get3DDataValue());
        tag.put("Filters", writeFilters(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.maxRate = Mth.clamp(tag.getInt("MaxRate"), 0, MAX_RATE);
        this.facing = Direction.from3DDataValue(tag.getInt("Facing"));
        readFilters(registries, tag.getList("Filters", Tag.TAG_COMPOUND));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("MaxRate", maxRate);
        tag.putInt("Facing", facing.get3DDataValue());
        tag.put("Filters", writeFilters(registries));
        return tag;
    }

    private ListTag writeFilters(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < filters.size(); i++) {
            FluidStack fluid = filters.get(i);
            if (fluid.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.put("Fluid", fluid.save(registries));
            list.add(entry);
        }
        return list;
    }

    private void readFilters(HolderLookup.Provider registries, ListTag list) {
        Collections.fill(filters, FluidStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < filters.size()) {
                filters.set(slot, FluidStack.parseOptional(registries, entry.getCompound("Fluid")));
            }
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sendUpdate() {
        if (this.level != null) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.control_valve");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ControlValveMenu(containerId, inventory, this);
    }
}
