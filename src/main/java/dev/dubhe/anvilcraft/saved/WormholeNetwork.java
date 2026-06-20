package dev.dubhe.anvilcraft.saved;

import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Global saved data that tracks all wormhole-stabilized Celestial Forging Anvil
 * positions across the entire server, enabling inter-dimensional wormhole connections.
 *
 * <p>
 * Two black holes produce identical {@code paramsHash} values and are therefore
 * "connected" — wormholes can form between them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WormholeNetwork extends BetterSavedData {

    /**
     * A single entry in the wormhole network, identifying one CFA.
     *
     * @param dimension      the dimension the CFA is in
     * @param pos            the BOTTOM_CENTER block position of the CFA
     * @param portalSides    which side centers have a Celestial Forging Anvil Portal placed
     */
    public record Entry(ResourceKey<Level> dimension, BlockPos pos, Set<Cube323PartHalf> portalSides) {

        Entry(ResourceKey<Level> dimension, BlockPos pos) {
            this(dimension, pos, Set.of());
        }

        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", dimension.location().toString());
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            if (!portalSides.isEmpty()) {
                ListTag sidesTag = new ListTag();
                for (Cube323PartHalf side : portalSides) {
                    CompoundTag sideTag = new CompoundTag();
                    sideTag.putString("side", side.getSerializedName());
                    sidesTag.add(sideTag);
                }
                tag.put("portalSides", sidesTag);
            }
            return tag;
        }

        static Entry fromTag(CompoundTag tag) {
            ResourceKey<Level> dim = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("dimension"))
            );
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            Set<Cube323PartHalf> sides = new HashSet<>();
            if (tag.contains("portalSides")) {
                ListTag sidesTag = tag.getList("portalSides", Tag.TAG_COMPOUND);
                for (int i = 0; i < sidesTag.size(); i++) {
                    CompoundTag sideTag = sidesTag.getCompound(i);
                    try {
                        sides.add(Cube323PartHalf.valueOf(sideTag.getString("side").toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                        // Unknown side — skip
                    }
                }
            }
            return new Entry(dim, pos, Set.copyOf(sides));
        }
    }

    /**
     * Map: parameterHash → list of CFA entries with matching parameters.
     */
    private final Map<Integer, List<Entry>> network = new HashMap<>();

    /**
     * Reverse index: dimension → (pos → hash), for O(1) unregistration.
     */
    private final Map<ResourceKey<Level>, Map<BlockPos, Integer>> reverseIndex = new HashMap<>();

    // ==================== Static accessors ====================

    /**
     * Get the server-side WormholeNetwork instance.
     */
    public static WormholeNetwork get() {
        return BetterSavedData.get("wormhole_network", WormholeNetwork::new);
    }

    // ==================== Registration ====================

    /**
     * Register a CFA in the network.
     *
     * @param paramsHash hash of the black hole's stable parameters
     * @param level      the dimension the CFA is in
     * @param pos        the BOTTOM_CENTER block position of the CFA
     */
    public void register(int paramsHash, Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        List<Entry> entries = network.computeIfAbsent(paramsHash, k -> new ArrayList<>());
        // Avoid duplicates — replace if already present
        entries.removeIf(e -> e.dimension.equals(dim) && e.pos.equals(pos));
        entries.add(new Entry(dim, pos));

        reverseIndex.computeIfAbsent(dim, k -> new HashMap<>()).put(pos, paramsHash);
        setDirty();
    }

    /**
     * Unregister a CFA from the network.
     */
    public void unregister(Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        Map<BlockPos, Integer> dimMap = reverseIndex.get(dim);
        if (dimMap == null) return;
        Integer hash = dimMap.remove(pos);
        if (hash != null) {
            List<Entry> entries = network.get(hash);
            if (entries != null) {
                entries.removeIf(e -> e.dimension.equals(dim) && e.pos.equals(pos));
                if (entries.isEmpty()) {
                    network.remove(hash);
                }
            }
            setDirty();
        }
    }

    // ==================== Portal side management ====================

    /**
     * Update the portal sides for a registered CFA entry.
     */
    public void setPortalSides(ResourceKey<Level> dim, BlockPos pos, Set<Cube323PartHalf> sides) {
        Integer hash = reverseIndex.getOrDefault(dim, Map.of()).get(pos);
        if (hash != null) {
            List<Entry> entries = network.get(hash);
            if (entries != null) {
                for (int i = 0; i < entries.size(); i++) {
                    Entry e = entries.get(i);
                    if (e.dimension.equals(dim) && e.pos.equals(pos)) {
                        entries.set(i, new Entry(e.dimension, e.pos, Set.copyOf(sides)));
                        setDirty();
                        return;
                    }
                }
            }
        }
    }

    /**
     * Check whether a specific side of a CFA has a portal.
     */
    public boolean hasPortalAt(ResourceKey<Level> dim, BlockPos pos, Cube323PartHalf side) {
        Integer hash = reverseIndex.getOrDefault(dim, Map.of()).get(pos);
        if (hash != null) {
            List<Entry> entries = network.get(hash);
            if (entries != null) {
                for (Entry e : entries) {
                    if (e.dimension.equals(dim) && e.pos.equals(pos)) {
                        return e.portalSides.contains(side);
                    }
                }
            }
        }
        return false;
    }

    // ==================== Queries ====================

    /**
     * Get all connected CFA entries (excluding self) for a given parameter hash.
     */
    public List<Entry> getConnected(int paramsHash, ResourceKey<Level> selfDim, BlockPos selfPos) {
        List<Entry> all = network.getOrDefault(paramsHash, List.of());
        return all.stream()
            .filter(e -> !(e.dimension.equals(selfDim) && e.pos.equals(selfPos)))
            .toList();
    }

    // ==================== Hash computation ====================

    /**
     * Compute a stable hash from a black hole's parameters.
     * Excludes dimension and position — identical black holes produce the same hash.
     */
    public static int computeParamsHash(StarData star) {
        // Manual hash combining for deterministic results across JVM runs
        int result = star.bodyClass().ordinal();
        result = 31 * result + star.size();
        result = 31 * result + star.colorR();
        result = 31 * result + star.colorG();
        result = 31 * result + star.colorB();
        result = 31 * result + Float.floatToIntBits(star.axialTilt());
        result = 31 * result + star.rotationSpeed();
        result = 31 * result + star.magneticFieldStrength();
        result = 31 * result + star.energy();
        return result;
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void registerDataFixers() {
        // No data fixers needed for initial version
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries) {
        network.clear();
        reverseIndex.clear();
        for (String key : nbt.getAllKeys()) {
            int hash;
            try {
                hash = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Entry entry = Entry.fromTag(list.getCompound(i));
                entries.add(entry);
                reverseIndex.computeIfAbsent(entry.dimension, k -> new HashMap<>())
                    .put(entry.pos, hash);
            }
            network.put(hash, entries);
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        for (Map.Entry<Integer, List<Entry>> entry : network.entrySet()) {
            ListTag list = new ListTag();
            for (Entry e : entry.getValue()) {
                list.add(e.toTag());
            }
            nbt.put(entry.getKey().toString(), list);
        }
        return nbt;
    }
}
