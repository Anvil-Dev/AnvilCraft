package dev.dubhe.anvilcraft.saved;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.saved.datafixers.DataFixers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
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
 * {@code paramsHash}, the relative block offset, and the interface type — receives a
 * deterministic {@link UUID}. All CFAs sharing the same wormhole network group access the
 * same canonical state, ensuring they behave like a single unified interface.
 *
 * <p>Laser interfaces are NOT stored here; their sync is purely computational
 * (sum inputs, split among outputs) each tick.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WormholeInterfaceStates extends BetterSavedData {

    private static final ResourceLocation FIXER_ID = AnvilCraft.of("wormhole_interface_states_fixers");
    private static final double CURRENT_VERSION = 0.0;
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
        return BetterSavedData.get("wormhole_interface_states", WormholeInterfaceStates::new);
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
        List<UnlimitedItemStack> state = itemStates.get(uuid);
        if (state == null) {
            state = new ArrayList<>(slotCount);
            for (int i = 0; i < slotCount; i++) {
                state.add(UnlimitedItemStack.EMPTY);
            }
            itemStates.put(uuid, state);
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
        List<FluidStack> state = fluidStates.get(uuid);
        if (state == null) {
            state = new ArrayList<>(tankCount);
            for (int i = 0; i < tankCount; i++) {
                state.add(FluidStack.EMPTY);
            }
            fluidStates.put(uuid, state);
            setDirty();
        }
        while (state.size() < tankCount) {
            state.add(FluidStack.EMPTY);
            setDirty();
        }
        return state;
    }

    public List<FluidStack> getFluidState(UUID uuid) {
        return fluidStates.get(uuid);
    }

    // ==================== DataFixers ====================

    @Override
    protected void registerDataFixers() {
        DataFixers.registerFixer(FIXER_ID);
    }

    // ==================== NBT Serialization ====================

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries) {
        itemStates.clear();
        fluidStates.clear();

        if (nbt.contains(ITEM_STATES_KEY)) {
            ListTag list = nbt.getList(ITEM_STATES_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                entryTag = DataFixers.fixData(FIXER_ID, CURRENT_VERSION, entryTag, registries);
                UUID uuid = entryTag.getUUID("uuid");
                int size = entryTag.getInt("size");
                ListTag slotsTag = entryTag.getList("slots", Tag.TAG_COMPOUND);
                List<UnlimitedItemStack> slots = new ArrayList<>(size);
                for (int j = 0; j < size; j++) {
                    slots.add(UnlimitedItemStack.EMPTY);
                }
                for (int j = 0; j < slotsTag.size(); j++) {
                    CompoundTag slotTag = slotsTag.getCompound(j);
                    int slotIdx = slotTag.getInt("Slot");
                    if (slotIdx >= 0 && slotIdx < size) {
                        UnlimitedItemStack.parse(registries, slotTag)
                            .ifPresent(stack -> slots.set(slotIdx, stack));
                    }
                }
                itemStates.put(uuid, slots);
            }
        }

        if (nbt.contains(FLUID_STATES_KEY)) {
            ListTag list = nbt.getList(FLUID_STATES_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                entryTag = DataFixers.fixData(FIXER_ID, CURRENT_VERSION, entryTag, registries);
                UUID uuid = entryTag.getUUID("uuid");
                int size = entryTag.getInt("size");
                ListTag tanksTag = entryTag.getList("tanks", Tag.TAG_COMPOUND);
                List<FluidStack> tanks = new ArrayList<>(size);
                for (int j = 0; j < size; j++) {
                    tanks.add(FluidStack.EMPTY);
                }
                for (int j = 0; j < tanksTag.size(); j++) {
                    CompoundTag tankTag = tanksTag.getCompound(j);
                    if (tankTag.getBoolean("Empty")) continue;
                    int tankIdx = tankTag.getInt("Tank");
                    if (tankIdx >= 0 && tankIdx < size) {
                        FluidStack.parse(registries, tankTag).ifPresent(fluid -> tanks.set(tankIdx, fluid));
                    }
                }
                fluidStates.put(uuid, tanks);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag itemList = new ListTag();
        for (var entry : itemStates.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("uuid", entry.getKey());
            entryTag.putInt("size", entry.getValue().size());
            ListTag slotsTag = new ListTag();
            for (int i = 0; i < entry.getValue().size(); i++) {
                UnlimitedItemStack stack = entry.getValue().get(i);
                if (stack.isEmpty()) continue;
                CompoundTag slotTag = stack.serializeNBT(registries);
                slotTag.putInt("Slot", i);
                slotsTag.add(slotTag);
            }
            entryTag.put("slots", slotsTag);
            itemList.add(entryTag);
        }
        nbt.put(ITEM_STATES_KEY, itemList);

        ListTag fluidList = new ListTag();
        for (var entry : fluidStates.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("uuid", entry.getKey());
            entryTag.putInt("size", entry.getValue().size());
            ListTag tanksTag = new ListTag();
            for (int i = 0; i < entry.getValue().size(); i++) {
                FluidStack fluid = entry.getValue().get(i);
                CompoundTag tankTag = new CompoundTag();
                tankTag.putInt("Tank", i);
                if (!fluid.isEmpty()) {
                    tankTag = (CompoundTag) fluid.save(registries, tankTag);
                } else {
                    tankTag.putBoolean("Empty", true);
                }
                tanksTag.add(tankTag);
            }
            entryTag.put("tanks", tanksTag);
            fluidList.add(entryTag);
        }
        nbt.put(FLUID_STATES_KEY, fluidList);

        return nbt;
    }
}
