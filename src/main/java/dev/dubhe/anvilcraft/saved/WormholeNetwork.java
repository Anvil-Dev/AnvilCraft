package dev.dubhe.anvilcraft.saved;

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
import java.util.UUID;


/**
 * Global saved data that tracks all wormhole-stabilized Celestial Forging Anvil
 * positions across the entire server, enabling inter-dimensional wormhole connections.
 *
 * <p>
 * CFAs are grouped by the black hole's {@code bodyUuid}. Only black holes that
 * originated from the same source (via singularity crystal snapshot) share the
 * same UUID and can form wormholes between them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WormholeNetwork extends BetterSavedData {

    /**
     * A single entry in the wormhole network, identifying one CFA.
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
                        // do nothing
                    }
                }
            }
            return new Entry(dim, pos, Set.copyOf(sides));
        }
    }

    /**
     * Map: bodyUuid → list of CFA entries sharing the same black hole identity.
     */
    private final Map<UUID, List<Entry>> network = new HashMap<>();

    /**
     * Reverse index: dimension → (pos → bodyUuid), for O(1) unregistration.
     */
    private final Map<ResourceKey<Level>, Map<BlockPos, UUID>> reverseIndex = new HashMap<>();

    // ==================== Static accessors ====================

    public static WormholeNetwork get() {
        return BetterSavedData.get("wormhole_network", WormholeNetwork::new);
    }

    // ==================== Registration ====================

    /**
     * Register a CFA in the network under the given black hole identity UUID.
     */
    public void register(UUID bodyUuid, Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        List<Entry> entries = network.computeIfAbsent(bodyUuid, k -> new ArrayList<>());
        entries.removeIf(e -> e.dimension.equals(dim) && e.pos.equals(pos));
        entries.add(new Entry(dim, pos));

        reverseIndex.computeIfAbsent(dim, k -> new HashMap<>()).put(pos, bodyUuid);
        setDirty();
    }

    /**
     * Unregister a CFA from the network.
     */
    public void unregister(Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        Map<BlockPos, UUID> dimMap = reverseIndex.get(dim);
        if (dimMap == null) return;
        UUID uuid = dimMap.remove(pos);
        if (uuid != null) {
            List<Entry> entries = network.get(uuid);
            if (entries != null) {
                entries.removeIf(e -> e.dimension.equals(dim) && e.pos.equals(pos));
                if (entries.isEmpty()) {
                    network.remove(uuid);
                }
            }
            setDirty();
        }
    }

    // ==================== Portal side management ====================

    public void setPortalSides(ResourceKey<Level> dim, BlockPos pos, Set<Cube323PartHalf> sides) {
        UUID uuid = reverseIndex.getOrDefault(dim, Map.of()).get(pos);
        if (uuid != null) {
            List<Entry> entries = network.get(uuid);
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

    public boolean hasPortalAt(ResourceKey<Level> dim, BlockPos pos, Cube323PartHalf side) {
        UUID uuid = reverseIndex.getOrDefault(dim, Map.of()).get(pos);
        if (uuid != null) {
            List<Entry> entries = network.get(uuid);
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
     * Get all connected CFA entries (excluding self) for a given body UUID.
     */
    public List<Entry> getConnected(UUID bodyUuid, ResourceKey<Level> selfDim, BlockPos selfPos) {
        List<Entry> all = network.getOrDefault(bodyUuid, List.of());
        return all.stream()
            .filter(e -> !(e.dimension.equals(selfDim) && e.pos.equals(selfPos)))
            .toList();
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void registerDataFixers() {
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries) {
        network.clear();
        reverseIndex.clear();
        for (String key : nbt.getAllKeys()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Entry entry = Entry.fromTag(list.getCompound(i));
                entries.add(entry);
                reverseIndex.computeIfAbsent(entry.dimension, k -> new HashMap<>())
                    .put(entry.pos, uuid);
            }
            network.put(uuid, entries);
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        for (Map.Entry<UUID, List<Entry>> entry : network.entrySet()) {
            ListTag list = new ListTag();
            for (Entry e : entry.getValue()) {
                list.add(e.toTag());
            }
            nbt.put(entry.getKey().toString(), list);
        }
        return nbt;
    }
}
