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
 * 每个槽位从事件暴露的 {@link BaseStorage} 中消耗一个物品，并随工作台、切石机一起记入该存储的
 * 消耗品记录；槽位下标非法或槽位为空时解锁失败且不消耗任何物品。</p>
 *
 * <p>取消事件可阻止本次解锁，取消时不消耗任何物品。</p>
 */
@Getter
public class StorageUnlockCraftingEvent extends Event implements ICancellableEvent {
    private final BaseStorage<?> storage;
    private final List<Integer> recipeBaseSlots = new ArrayList<>();

    public StorageUnlockCraftingEvent(BaseStorage<?> storage) {
        this.storage = storage;
    }

    /**
     * 追加一个解锁要消耗的槽位。
     *
     * @param slot 储物内容中的槽位下标，每个槽位消耗一个物品
     */
    public void addRecipeBaseSlot(int slot) {
        this.recipeBaseSlots.add(slot);
    }

    /**
     * 本次解锁要消耗的槽位，包含工作台与切石机的槽位。
     *
     * @return 槽位下标的不可修改视图
     */
    public List<Integer> getRecipeBaseSlots() {
        return Collections.unmodifiableList(this.recipeBaseSlots);
    }
}
