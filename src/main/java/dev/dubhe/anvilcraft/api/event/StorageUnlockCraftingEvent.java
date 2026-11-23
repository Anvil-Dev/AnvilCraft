package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 仓储解锁合成功能事件。
 *
 * <p>事件在扣除消耗品前发出，其它模组可通过 {@link #addRecipeBaseSlot(int)} 追加消耗槽位，
 * 每个槽位从对应的来源存储中消耗一个物品，并随工作台、切石机一起记入当前存储的
 * 消耗品记录；槽位下标非法或槽位为空时解锁失败且不消耗任何物品。</p>
 *
 * <p>取消事件可阻止本次解锁，取消时不消耗任何物品。</p>
 */
@Getter
public class StorageUnlockCraftingEvent extends Event implements ICancellableEvent {
    private final BaseStorage<?> storage;
    private final List<SourceSlot> sources = new ArrayList<>();

    /** 解锁消耗品在来源存储中的槽位。 */
    public record SourceSlot(BaseStorage<?> storage, int slot) {
    }

    public StorageUnlockCraftingEvent(BaseStorage<?> storage) {
        this.storage = storage;
    }

    /**
     * 追加一个解锁要消耗的槽位。
     *
     * @param slot 储物内容中的槽位下标，每个槽位消耗一个物品
     */
    public void addRecipeBaseSlot(int slot) {
        this.addRecipeBaseSlot(this.storage, slot);
    }

    /** 指定参与本次解锁的存储及消耗槽位。 */
    public void addRecipeBaseSlot(BaseStorage<?> source, int slot) {
        this.sources.add(new SourceSlot(source, slot));
    }

    /** 返回全部消耗槽位及其来源存储。 */
    public List<SourceSlot> getSources() {
        return Collections.unmodifiableList(this.sources);
    }

    /**
     * 当前打开的存储中要消耗的槽位。
     *
     * @return 槽位下标的不可修改视图
     */
    public List<Integer> getRecipeBaseSlots() {
        return this.sources.stream().filter(source -> source.storage() == this.storage).map(SourceSlot::slot).toList();
    }
}
