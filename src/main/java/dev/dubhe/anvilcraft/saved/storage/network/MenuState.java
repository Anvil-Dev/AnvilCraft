package dev.dubhe.anvilcraft.saved.storage.network;

import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// 双端界面状态，用于同步双端界面数据
public class MenuState {
    private static final Map<UUID, MenuState> STATES = new HashMap<>();

    public static MenuState get(UUID id) {
        return MenuState.STATES.computeIfAbsent(id, MenuState::new);
    }

    public static void clear() {
        MenuState.STATES.clear();
    }

    private final UUID id;
    private final IntList slots;
    private final ArrayList<UnlimitedItemStack> mapping;

    public MenuState(UUID id) {
        this.id = id;
        this.slots = new IntArrayList();
        this.mapping = new ArrayList<>();
    }

    public void sync(IntList slots) {
        this.slots.clear();
        this.slots.addAll(slots);
    }

    public void sync(int head, List<UnlimitedItemStack> stacks) {
        this.mapping.ensureCapacity(head);

        int size = stacks.size();
        for (int i = head; i < head + size; i++) {
            if (this.mapping.get(i) == null) {
                this.mapping.add(i, stacks.get(i));
            } else {
                this.mapping.set(i, stacks.get(i));
            }
        }
    }

    public void sync(Map<Integer, UnlimitedItemStack> stacks) {
        for (int index : stacks.keySet()) {
            this.mapping.ensureCapacity(index);
            this.mapping.set(index, stacks.get(index));
        }
    }
}
