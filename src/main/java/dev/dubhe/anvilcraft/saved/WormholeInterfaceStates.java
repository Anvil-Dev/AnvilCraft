package dev.dubhe.anvilcraft.saved;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 保存每组虫洞物流和流体接口权威状态的全局存档数据。
 * 每组接口由虫洞天体标识、相对方块偏移和接口类型生成确定性的 {@link UUID}，
 * 同一虫洞网络中的全部锻星砧共享该状态，因此表现为一个统一接口。
 * 激光接口不在此持久化，其输入汇总与输出分配会在每刻重新计算。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WormholeInterfaceStates extends BetterSavedData {

    static final WormholeInterfaceStates CLIENT_COPY = new WormholeInterfaceStates();

    public static final Codec<WormholeInterfaceStates> CODEC = CompoundTag.CODEC.comapFlatMap(
        tag -> {
            WormholeInterfaceStates states = new WormholeInterfaceStates();
            states.readFromTag(tag);
            return DataResult.success(states);
        },
        states -> {
            CompoundTag tag = new CompoundTag();
            states.writeToTag(tag);
            return tag;
        }
    );

    public static final SavedDataType<WormholeInterfaceStates> TYPE = new SavedDataType<>(
        AnvilCraft.of("wormhole_interface_states"),
        WormholeInterfaceStates::new,
        WormholeInterfaceStates.CODEC,
        null
    );

    private static final String ITEM_STATES_KEY = "itemStates";
    private static final String FLUID_STATES_KEY = "fluidStates";
    private static final String TYPE_LOGISTICS = "logistics";
    private static final String TYPE_FLUID = "fluid";

    /** 物流接口标识到槽位列表的映射，空槽使用 {@link UnlimitedItemStack#EMPTY}。 */
    private final Map<UUID, List<UnlimitedItemStack>> itemStates = new HashMap<>();

    /** 流体接口标识到储罐列表的映射，空储罐使用 {@link FluidStack#EMPTY}。 */
    private final Map<UUID, List<FluidStack>> fluidStates = new HashMap<>();

    // ==================== 静态访问 ====================

    public static WormholeInterfaceStates get() {
        return BetterSavedData.get(WormholeInterfaceStates.TYPE, WormholeInterfaceStates.CLIENT_COPY);
    }

    // ==================== 接口标识生成 ====================

    /**
     * 为一组接口生成确定性标识，只有源自同一黑洞天体的锻星砧副本才会共享状态。
     */
    public static UUID interfaceUuid(UUID bodyUuid, int relX, int relZ, String type) {
        String input = "wormhole:" + bodyUuid + ":" + relX + ":" + relZ + ":" + type;
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    public static UUID logisticsUuid(UUID bodyUuid, int relX, int relZ) {
        return WormholeInterfaceStates.interfaceUuid(bodyUuid, relX, relZ, WormholeInterfaceStates.TYPE_LOGISTICS);
    }

    public static UUID fluidUuid(UUID bodyUuid, int relX, int relZ) {
        return WormholeInterfaceStates.interfaceUuid(bodyUuid, relX, relZ, WormholeInterfaceStates.TYPE_FLUID);
    }

    // ==================== 物品状态访问 ====================

    /**
     * 获取或创建指定标识的权威物品状态。返回列表可修改，结构变化后必须调用 {@link #setDirty()}。
     */
    public List<UnlimitedItemStack> getOrCreateItemState(UUID uuid, int slotCount) {
        List<UnlimitedItemStack> state = this.itemStates.get(uuid);
        if (state == null) {
            state = new ArrayList<>(slotCount);
            for (int i = 0; i < slotCount; i++) {
                state.add(UnlimitedItemStack.EMPTY);
            }
            this.itemStates.put(uuid, state);
            this.setDirty();
        }
        while (state.size() < slotCount) {
            state.add(UnlimitedItemStack.EMPTY);
            this.setDirty();
        }
        return state;
    }

    // ==================== 流体状态访问 ====================

    /**
     * 获取或创建指定标识的权威流体状态。返回列表可修改，结构变化后必须调用 {@link #setDirty()}。
     */
    public List<FluidStack> getOrCreateFluidState(UUID uuid, int tankCount) {
        List<FluidStack> state = this.fluidStates.get(uuid);
        if (state == null) {
            state = new ArrayList<>(tankCount);
            for (int i = 0; i < tankCount; i++) {
                state.add(FluidStack.EMPTY);
            }
            this.fluidStates.put(uuid, state);
            this.setDirty();
        }
        while (state.size() < tankCount) {
            state.add(FluidStack.EMPTY);
            this.setDirty();
        }
        return state;
    }

    public List<FluidStack> getFluidState(UUID uuid) {
        return this.fluidStates.get(uuid);
    }

    // ==================== 编解码器使用的 NBT 序列化 ====================

    private void writeToTag(CompoundTag nbt) {
        if (!this.itemStates.isEmpty()) {
            CompoundTag itemsTag = new CompoundTag();
            for (var entry : this.itemStates.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                ListTag slotsTag = new ListTag();
                List<UnlimitedItemStack> slots = entry.getValue();
                entryTag.putInt("size", slots.size());
                for (int i = 0; i < slots.size(); i++) {
                    UnlimitedItemStack stack = slots.get(i);
                    if (stack.isEmpty()) continue;
                    final int slotIdx = i;
                    UnlimitedItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)
                        .resultOrPartial()
                        .ifPresent(tag -> {
                            if (tag instanceof CompoundTag stackTag) {
                                stackTag.putInt("Slot", slotIdx);
                                slotsTag.add(stackTag);
                            }
                        });
                }
                entryTag.put("slots", slotsTag);
                itemsTag.put(entry.getKey().toString(), entryTag);
            }
            nbt.put(WormholeInterfaceStates.ITEM_STATES_KEY, itemsTag);
        }

        if (!this.fluidStates.isEmpty()) {
            CompoundTag fluidsTag = new CompoundTag();
            for (var entry : this.fluidStates.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                ListTag tanksTag = new ListTag();
                List<FluidStack> tanks = entry.getValue();
                entryTag.putInt("size", tanks.size());
                for (int i = 0; i < tanks.size(); i++) {
                    FluidStack fluid = tanks.get(i);
                    if (fluid.isEmpty()) continue;
                    final int tankIdx = i;
                    FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, fluid)
                        .resultOrPartial()
                        .ifPresent(tag -> {
                            if (tag instanceof CompoundTag tankTag) {
                                tankTag.putInt("Tank", tankIdx);
                                tanksTag.add(tankTag);
                            }
                        });
                }
                entryTag.put("tanks", tanksTag);
                fluidsTag.put(entry.getKey().toString(), entryTag);
            }
            nbt.put(WormholeInterfaceStates.FLUID_STATES_KEY, fluidsTag);
        }
    }

    private void readFromTag(CompoundTag nbt) {
        this.itemStates.clear();
        this.fluidStates.clear();

        if (nbt.contains(WormholeInterfaceStates.ITEM_STATES_KEY)) {
            CompoundTag itemsTag = nbt.getCompoundOrEmpty(WormholeInterfaceStates.ITEM_STATES_KEY);
            for (String key : itemsTag.keySet()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                CompoundTag entryTag = itemsTag.getCompoundOrEmpty(key);
                int size = entryTag.getIntOr("size", 0);
                ListTag slotsTag = entryTag.getListOrEmpty("slots");
                List<UnlimitedItemStack> slots = new ArrayList<>(size);
                for (int j = 0; j < size; j++) {
                    slots.add(UnlimitedItemStack.EMPTY);
                }
                for (int j = 0; j < slotsTag.size(); j++) {
                    CompoundTag slotTag = slotsTag.getCompoundOrEmpty(j);
                    int slotIdx = slotTag.getIntOr("Slot", -1);
                    if (slotIdx >= 0 && slotIdx < size) {
                        UnlimitedItemStack.CODEC.parse(NbtOps.INSTANCE, slotTag)
                            .resultOrPartial()
                            .ifPresent(stack -> slots.set(slotIdx, stack));
                    }
                }
                this.itemStates.put(uuid, slots);
            }
        }

        if (nbt.contains(WormholeInterfaceStates.FLUID_STATES_KEY)) {
            CompoundTag fluidsTag = nbt.getCompoundOrEmpty(WormholeInterfaceStates.FLUID_STATES_KEY);
            for (String key : fluidsTag.keySet()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                CompoundTag entryTag = fluidsTag.getCompoundOrEmpty(key);
                int size = entryTag.getIntOr("size", 0);
                ListTag tanksTag = entryTag.getListOrEmpty("tanks");
                List<FluidStack> tanks = new ArrayList<>(size);
                for (int j = 0; j < size; j++) {
                    tanks.add(FluidStack.EMPTY);
                }
                for (int j = 0; j < tanksTag.size(); j++) {
                    CompoundTag tankTag = tanksTag.getCompoundOrEmpty(j);
                    int tankIdx = tankTag.getIntOr("Tank", -1);
                    if (tankIdx >= 0 && tankIdx < size) {
                        FluidStack.CODEC.parse(NbtOps.INSTANCE, tankTag)
                            .resultOrPartial()
                            .ifPresent(fluid -> tanks.set(tankIdx, fluid));
                    }
                }
                this.fluidStates.put(uuid, tanks);
            }
        }
    }

    // ==================== 存档数据抽象方法 ====================

    @Override
    protected void registerDataFixers() {
    }

    @Override
    protected @Nullable Packet<? extends CustomPacketPayload> createPacket(
        RegistryAccess registryAccess
    ) {
        // 虫洞接口状态仅在服务端使用，不向客户端同步。
        return null;
    }
}
