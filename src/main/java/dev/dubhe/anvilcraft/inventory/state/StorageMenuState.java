package dev.dubhe.anvilcraft.inventory.state;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.anvilcraft.lib.v2.util.Util;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// 双端界面状态，用于同步双端界面数据
@Getter
@Setter
public class StorageMenuState {
    private static final Map<UUID, StorageMenuState> STATES = new HashMap<>();

    public static StorageMenuState get(UUID id) {
        return StorageMenuState.STATES.computeIfAbsent(id, StorageMenuState::new);
    }

    public static void clear(UUID id) {
        StorageMenuState.STATES.remove(id);
    }

    public static void clear() {
        StorageMenuState.STATES.clear();
    }

    private final UUID id;
    private final IntList slots;
    private final ArrayList<UnlimitedItemStack> mapping;
    private final Map<Integer, UnlimitedItemStack> changes;
    private double fullness;

    public StorageMenuState(UUID id) {
        this.id = id;
        this.slots = new IntArrayList();
        this.mapping = new ArrayList<>();
        this.changes = new HashMap<>();
    }

    /// 获取服务端需要更新的列表
    ///
    /// **注意：不允许在客户端调用该方法**
    ///
    /// @return 服务端需要更新的列表
    public Map<Integer, UnlimitedItemStack> getChanges() {
        if (!Util.isServer()) {
            throw new IllegalStateException("Cannot invoke StorageMenuState#getChanges in clientside.");
        }
        return this.changes;
    }

    public void sync(IntList slots) {
        this.slots.clear();
        this.slots.addAll(slots);
    }

    public void sync(int head, List<UnlimitedItemStack> stacks) {
        while (this.mapping.size() <= head) {
            this.mapping.add(UnlimitedItemStack.EMPTY);
        }

        int size = stacks.size();
        for (int i = head; i < head + size; i++) {
            if (ListUtil.safelyGet(this.mapping, i).isEmpty()) {
                this.mapping.add(i, stacks.get(i - head));
            } else {
                this.mapping.set(i, stacks.get(i - head));
            }
        }
    }

    public void sync(Map<Integer, UnlimitedItemStack> stacks) {
        for (int index : stacks.keySet()) {
            while (this.mapping.size() <= index) {
                this.mapping.add(UnlimitedItemStack.EMPTY);
            }
            this.mapping.set(index, stacks.get(index));
        }
    }
}
