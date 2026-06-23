package dev.dubhe.anvilcraft.saved;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global saved data that stores the canonical state for each wormhole interface pair
 * (logistics items &amp; fluid tanks). Each pair — identified by the wormhole network's
 * {@code bodyUuid}, the relative block offset, and the interface type — receives a
 * deterministic {@link UUID}. All CFAs sharing the same wormhole network group access the
 * same canonical state, ensuring they behave like a single unified interface.
 *
 * <p>Laser interfaces are NOT stored here; their sync is purely computational
 * (sum inputs, split among outputs) each tick.</p>
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

    /**
     * UUID → slot list for logistics (item) interfaces.
     * Each list position corresponds to a slot index. Empty slots are UnlimitedItemStack.EMPTY.
     */
    private final Map<UUID, List<UnlimitedItemStack>> itemStates = new HashMap<>();

    /**
     * UUID → tank list for fluid interfaces.
     * Each list position corresponds to a tank index. Empty tanks are FluidStack.EMPTY.
     */
    private final Map<UUID, List<FluidStack>> fluidStates = new HashMap<>();

    // ==================== Static accessors ====================

    public static WormholeInterfaceStates get() {
        return BetterSavedData.get(TYPE, CLIENT_COPY);
    }

    // ==================== UUID generation ====================

    /**
     * Generate a deterministic UUID for an interface pair.
     * Uses the black hole's body UUID so only CFA copies from the same source share state.
     */
    public static UUID interfaceUuid(UUID bodyUuid, int relX, int relZ, String type) {
        String input = "wormhole:" + bodyUuid + ":" + relX + ":" + relZ + ":" + type;
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    public static UUID logisticsUuid(UUID bodyUuid, int relX, int relZ) {
        return interfaceUuid(bodyUuid, relX, relZ, TYPE_LOGISTICS);
    }

    public static UUID fluidUuid(UUID bodyUuid, int relX, int relZ) {
        return interfaceUuid(bodyUuid, relX, relZ, TYPE_FLUID);
    }

    // ==================== Item state access ====================

    /**
     * Get or create the canonical item state for a UUID.
     * The returned list is mutable; modifications are reflected in the saved data.
     * Call {@link #setDirty()} after structural changes.
     */
    public List<UnlimitedItemStack> getOrCreateItemState(UUID uuid, int slotCount) {
        List<UnlimitedItemStack> state = this.itemStates.get(uuid);
        if (state == null) {
            state = new ArrayList<>(slotCount);
            for (int i = 0; i < slotCount; i++) {
                state.add(UnlimitedItemStack.EMPTY);
            }
            this.itemStates.put(uuid, state);
            setDirty();
        }
        while (state.size() < slotCount) {
            state.add(UnlimitedItemStack.EMPTY);
            setDirty();
        }
        return state;
    }

    // ==================== Fluid state access ====================

    /**
     * Get or create the canonical fluid state for a UUID.
     * The returned list is mutable; modifications are reflected in the saved data.
     * Call {@link #setDirty()} after structural changes.
     */
    public List<FluidStack> getOrCreateFluidState(UUID uuid, int tankCount) {
        List<FluidStack> state = this.fluidStates.get(uuid);
        if (state == null) {
            state = new ArrayList<>(tankCount);
            for (int i = 0; i < tankCount; i++) {
                state.add(FluidStack.EMPTY);
            }
            this.fluidStates.put(uuid, state);
            setDirty();
        }
        while (state.size() < tankCount) {
            state.add(FluidStack.EMPTY);
            setDirty();
        }
        return state;
    }

    public List<FluidStack> getFluidState(UUID uuid) {
        return this.fluidStates.get(uuid);
    }

    // ==================== NBT Serialization (used by Codec) ====================

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
            nbt.put(ITEM_STATES_KEY, itemsTag);
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
            nbt.put(FLUID_STATES_KEY, fluidsTag);
        }
    }

    private void readFromTag(CompoundTag nbt) {
        this.itemStates.clear();
        this.fluidStates.clear();

        if (nbt.contains(ITEM_STATES_KEY)) {
            CompoundTag itemsTag = nbt.getCompoundOrEmpty(ITEM_STATES_KEY);
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

        if (nbt.contains(FLUID_STATES_KEY)) {
            CompoundTag fluidsTag = nbt.getCompoundOrEmpty(FLUID_STATES_KEY);
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

    // ==================== BetterSavedData abstract methods ====================

    @Override
    protected void registerDataFixers() {
    }

    @Override
    protected Packet<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> createPacket(
        RegistryAccess registryAccess
    ) {
        // Server-side only; wormhole interface states are not synced to clients.
        return null;
    }
}
