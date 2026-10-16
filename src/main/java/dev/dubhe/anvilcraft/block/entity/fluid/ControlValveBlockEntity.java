package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 控制阀的 BlockEntity。存储流体过滤（白名单）和最大流速，供流体网络分配时门控。
 *
 * <h3>过滤</h3>
 * 白名单直接以 {@link FluidStack} 存储（<b>不经桶物品</b>，因为部分流体如蜂蜜无对应桶），
 * 由玩家在 GUI 中手动放入桶 / 从 JEI 拖入流体设置，此为<b>单流体白名单</b>。
 *
 * <p>过滤格还可放入<b>过滤器物品</b>以一次过滤多种流体：此时逐个读取过滤器内容，
 * 把其中的流体桶（以及附魔书——液态魔咒无对应桶）解析为流体，并按过滤器的
 * 白名单 / 黑名单模式决定放行或拦截。放入过滤器后单流体白名单不再参与判定。
 * 未设置任何过滤 → 允许所有流体通过。</p>
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
    /** 过滤器物品的 NBT 键 */
    private static final String FILTER_ITEM_NBT_ID = "FilterItem";

    /** 白名单过滤流体（每个"槽"一种；{@link FluidStack#EMPTY} 表示该槽未设置） */
    private final NonNullList<FluidStack> filters = NonNullList.withSize(FILTER_SLOT_COUNT, FluidStack.EMPTY);

    /**
     * 过滤格中放入的过滤器物品（{@link ItemStack#EMPTY} 表示未放入）。
     *
     * <p>放入过滤器时白名单改由过滤器内容解析，且沿用过滤器的白名单 / 黑名单模式。</p>
     */
    private ItemStack filterItem = ItemStack.EMPTY;

    /** {@link #getFluidFilter()} 的缓存；{@code null} 表示待重算。 */
    private @Nullable FluidFilter cachedFilter;

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
        return this.isLocked() ? 0 : this.maxRate;
    }

    /** 设置某槽的过滤流体（数量归一，仅记录种类）。 */
    public void setFilter(int index, FluidStack fluid) {
        if (index < 0 || index >= this.filters.size()) {
            return;
        }
        this.filters.set(index, fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1));
        this.cachedFilter = null;
        this.setChanged();
    }

    public FluidStack getFilter(int index) {
        return (index < 0 || index >= this.filters.size()) ? FluidStack.EMPTY : this.filters.get(index);
    }

    /** 设置过滤格中的过滤器物品（空物品表示取出过滤器，回到单流体白名单模式）。 */
    public void setFilterItem(ItemStack stack) {
        this.filterItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.cachedFilter = null;
        this.setChanged();
    }

    /**
     * 解析一个物品所代表的过滤流体：流体桶取其内容物，「附魔书 / 书」按液态魔咒取。
     *
     * @param stack 过滤器内的一个物品
     * @return 对应的流体，无法解析时返回 {@link FluidStack#EMPTY}
     */
    public static FluidStack fluidOf(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(stack.copyWithCount(1)).orElse(null);
        return handler == null ? FluidStack.EMPTY : handler.getFluidInTank(0);
    }

    /**
     * 获取当前过滤生效的全部流体。
     *
     * @return 过滤涉及的流体列表，空列表表示未设置过滤
     */
    public List<FluidStack> getFilteredFluids() {
        return this.getFluidFilter().fluids();
    }

    /**
     * 获取当前生效的过滤规则（流体列表 + 白/黑名单模式）。
     *
     * @return 过滤规则；{@code fluids} 为空表示未设置过滤（全部放行）
     */
    public FluidFilter getFluidFilter() {
        if (this.cachedFilter != null) return this.cachedFilter;
        this.cachedFilter = this.computeFluidFilter();
        return this.cachedFilter;
    }

    private FluidFilter computeFluidFilter() {
        if (this.filterItem.isEmpty()) {
            // 手动放入的单流体白名单：始终为白名单
            List<FluidStack> single = new ArrayList<>(FILTER_SLOT_COUNT);
            for (FluidStack fluid : this.filters) {
                addFluid(single, fluid);
            }
            return new FluidFilter(List.copyOf(single), false);
        }
        FilterContent content = this.filterItem.get(ModComponents.FILTER_CONTENT);
        return new FluidFilter(List.copyOf(resolveFluids(this.filterItem)), content != null && content.denyList());
    }

    /**
     * 解析过滤器物品中所有可识别的流体（流体桶 / 附魔书）。
     *
     * <p>供服务端判定与客户端渲染共用，避免两侧解析规则分叉。</p>
     *
     * @param filter 过滤器物品，可为空
     * @return 去重后的流体列表（数量归一为 1）
     */
    public static List<FluidStack> resolveFluids(ItemStack filter) {
        List<FluidStack> result = new ArrayList<>();
        if (filter.isEmpty()) return result;
        FilterContent content = filter.get(ModComponents.FILTER_CONTENT);
        if (content == null) {
            addFluid(result, fluidOf(filter));
            return result;
        }
        for (ItemStack entry : content.list()) {
            if (entry.isEmpty()) continue;
            addFluid(result, fluidOf(entry));
        }
        return result;
    }

    /**
     * 获取当前用于渲染轮播的过滤流体。
     *
     * <p>多个流体按 {@code ui.filterItemDisplayIntervalMillis} 依次轮播，与过滤器物品自身的展示节奏一致。</p>
     *
     * @param timeMillis 当前时间戳
     * @return 本次应当渲染的流体，无过滤时返回 {@link FluidStack#EMPTY}
     */
    public FluidStack getDisplayedFilterFluid(long timeMillis) {
        List<FluidStack> fluids = this.getFilteredFluids();
        if (fluids.isEmpty()) return FluidStack.EMPTY;
        int interval = AnvilCraft.CLIENT_CONFIG.ui.filterItemDisplayIntervalMillis;
        int index = (int) Math.floorMod(timeMillis / Math.max(1, interval), fluids.size());
        return fluids.get(index);
    }

    private static void addFluid(List<FluidStack> result, FluidStack fluid) {
        if (fluid.isEmpty()) return;
        for (FluidStack existing : result) {
            if (FluidStack.isSameFluidSameComponents(existing, fluid)) return;
        }
        result.add(fluid.copyWithAmount(1));
    }

    /**
     * 判断某流体是否被允许通过本阀门。
     *
     * @return 未设置过滤 → 全部允许；白名单 → 仅列表内放行；黑名单 → 列表内拦截
     */
    public boolean allows(FluidStack fluid) {
        return this.getFluidFilter().allows(fluid);
    }

    /**
     * 解析后的过滤规则。
     *
     * @param fluids   过滤涉及的流体；为空表示未设置过滤
     * @param denyList 是否为黑名单模式
     */
    public record FluidFilter(List<FluidStack> fluids, boolean denyList) {
        /** 该流体是否允许通过：未设置过滤全放行；白名单命中放行；黑名单命中拦截。 */
        public boolean allows(FluidStack fluid) {
            if (this.fluids.isEmpty()) return true;
            boolean matched = false;
            for (FluidStack entry : this.fluids) {
                if (FluidStack.isSameFluidSameComponents(entry, fluid)) {
                    matched = true;
                    break;
                }
            }
            return this.denyList != matched;
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MaxRate", this.maxRate);
        tag.putInt("Facing", this.facing.get3DDataValue());
        tag.put("Filters", this.writeFilters(registries));
        this.writeFilterItem(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.maxRate = Mth.clamp(tag.getInt("MaxRate"), 0, MAX_RATE);
        this.facing = Direction.from3DDataValue(tag.getInt("Facing"));
        this.readFilters(registries, tag.getList("Filters", Tag.TAG_COMPOUND));
        this.filterItem = tag.contains(FILTER_ITEM_NBT_ID)
            ? ItemStack.parseOptional(registries, tag.getCompound(FILTER_ITEM_NBT_ID))
            : ItemStack.EMPTY;
        this.cachedFilter = null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("MaxRate", this.maxRate);
        tag.putInt("Facing", this.facing.get3DDataValue());
        tag.put("Filters", this.writeFilters(registries));
        this.writeFilterItem(tag, registries);
        return tag;
    }

    private void writeFilterItem(CompoundTag tag, HolderLookup.Provider registries) {
        if (!this.filterItem.isEmpty()) {
            tag.put(FILTER_ITEM_NBT_ID, this.filterItem.save(registries));
        }
    }

    private ListTag writeFilters(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < this.filters.size(); i++) {
            FluidStack fluid = this.filters.get(i);
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
        Collections.fill(this.filters, FluidStack.EMPTY);
        this.cachedFilter = null;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < this.filters.size()) {
                this.filters.set(slot, FluidStack.parseOptional(registries, entry.getCompound("Fluid")));
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
