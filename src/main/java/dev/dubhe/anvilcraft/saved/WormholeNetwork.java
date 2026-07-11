package dev.dubhe.anvilcraft.saved;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 记录整个服务端中全部虫洞稳定锻星砧位置的全局存档数据，用于建立跨维度虫洞连接。
 * 锻星砧按黑洞的 {@code bodyUuid} 分组，只有来自同一天体快照的黑洞共享标识并能互相连接。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WormholeNetwork extends BetterSavedData {

    static final WormholeNetwork CLIENT_COPY = new WormholeNetwork();

    public static final Codec<WormholeNetwork> CODEC = CompoundTag.CODEC.comapFlatMap(
        tag -> {
            WormholeNetwork net = new WormholeNetwork();
            net.readFromTag(tag);
            return DataResult.success(net);
        },
        net -> {
            CompoundTag tag = new CompoundTag();
            net.writeToTag(tag);
            return tag;
        }
    );

    public static final SavedDataType<WormholeNetwork> TYPE = new SavedDataType<>(
        AnvilCraft.of("wormhole_network"),
        WormholeNetwork::new,
        WormholeNetwork.CODEC,
        null
    );

    /** 虫洞网络中的单个锻星砧条目。 */
    public record Entry(ResourceKey<Level> dimension, BlockPos pos, Set<Cube323PartHalf> portalSides) {

        Entry(ResourceKey<Level> dimension, BlockPos pos) {
            this(dimension, pos, Set.of());
        }

        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", this.dimension.identifier().toString());
            tag.putInt("x", this.pos.getX());
            tag.putInt("y", this.pos.getY());
            tag.putInt("z", this.pos.getZ());
            if (!this.portalSides.isEmpty()) {
                ListTag sidesTag = new ListTag();
                for (Cube323PartHalf side : this.portalSides) {
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
                Registries.DIMENSION,
                Identifier.parse(tag.getStringOr("dimension", ""))
            );
            BlockPos pos = new BlockPos(
                tag.getIntOr("x", 0),
                tag.getIntOr("y", 0),
                tag.getIntOr("z", 0)
            );
            Set<Cube323PartHalf> sides = new HashSet<>();
            if (tag.contains("portalSides")) {
                ListTag sidesTag = tag.getListOrEmpty("portalSides");
                for (int i = 0; i < sidesTag.size(); i++) {
                    CompoundTag sideTag = sidesTag.getCompoundOrEmpty(i);
                    String sideName = sideTag.getStringOr("side", "");
                    for (Cube323PartHalf side : Cube323PartHalf.values()) {
                        if (!side.name().equalsIgnoreCase(sideName)) continue;
                        sides.add(side);
                        break;
                    }
                }
            }
            return new Entry(dim, pos, Set.copyOf(sides));
        }
    }

    /** 天体标识到共享同一黑洞身份的锻星砧条目列表。 */
    private final Map<UUID, List<Entry>> network = new HashMap<>();

    /** 维度和位置到天体标识的反向索引，用于常数时间注销。 */
    private final Map<ResourceKey<Level>, Map<BlockPos, UUID>> reverseIndex = new HashMap<>();

    // ==================== 静态访问 ====================

    public static WormholeNetwork get() {
        return BetterSavedData.get(TYPE, CLIENT_COPY);
    }

    // ==================== 注册与注销 ====================

    /** 使用指定黑洞身份标识将锻星砧注册到虫洞网络。 */
    public void register(UUID bodyUuid, Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        List<Entry> entries = this.network.computeIfAbsent(bodyUuid, k -> new ArrayList<>());
        entries.removeIf(e -> e.dimension.equals(dim) && e.pos.equals(pos));
        entries.add(new Entry(dim, pos));

        this.reverseIndex.computeIfAbsent(dim, k -> new HashMap<>()).put(pos, bodyUuid);
        setDirty();
    }

    /** 从虫洞网络注销锻星砧。 */
    public void unregister(Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        Map<BlockPos, UUID> dimMap = this.reverseIndex.get(dim);
        if (dimMap == null) return;
        UUID uuid = dimMap.remove(pos);
        if (uuid != null) {
            List<Entry> entries = this.network.get(uuid);
            if (entries != null) {
                entries.removeIf(e -> e.dimension.equals(dim) && e.pos.equals(pos));
                if (entries.isEmpty()) {
                    this.network.remove(uuid);
                }
            }
            setDirty();
        }
    }

    // ==================== 传送门侧面管理 ====================

    public void setPortalSides(ResourceKey<Level> dim, BlockPos pos, Set<Cube323PartHalf> sides) {
        UUID uuid = this.reverseIndex.getOrDefault(dim, Map.of()).get(pos);
        if (uuid != null) {
            List<Entry> entries = this.network.get(uuid);
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
        UUID uuid = this.reverseIndex.getOrDefault(dim, Map.of()).get(pos);
        if (uuid != null) {
            List<Entry> entries = this.network.get(uuid);
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

    // ==================== 查询 ====================

    /** 获取指定天体标识下除自身外的全部已连接锻星砧条目。 */
    public List<Entry> getConnected(UUID bodyUuid, ResourceKey<Level> selfDim, BlockPos selfPos) {
        List<Entry> all = this.network.getOrDefault(bodyUuid, List.of());
        return all.stream()
            .filter(e -> !(e.dimension.equals(selfDim) && e.pos.equals(selfPos)))
            .toList();
    }

    // ==================== 编解码器使用的 NBT 序列化 ====================

    private void writeToTag(CompoundTag nbt) {
        for (Map.Entry<UUID, List<Entry>> entry : this.network.entrySet()) {
            ListTag list = new ListTag();
            for (Entry e : entry.getValue()) {
                list.add(e.toTag());
            }
            nbt.put(entry.getKey().toString(), list);
        }
    }

    private void readFromTag(CompoundTag nbt) {
        this.network.clear();
        this.reverseIndex.clear();
        for (String key : nbt.keySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ListTag list = nbt.getListOrEmpty(key);
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Entry entry = Entry.fromTag(list.getCompoundOrEmpty(i));
                entries.add(entry);
                this.reverseIndex.computeIfAbsent(entry.dimension, k -> new HashMap<>())
                    .put(entry.pos, uuid);
            }
            this.network.put(uuid, entries);
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
        // WormholeNetwork is server-side-only saved data. Client sync is not required
        // because wormhole connections are managed entirely through server-side NBT
        // persistence (via Codec-based SavedDataType). The network is queried only
        // during server tick for cross-CFA synchronization.
        // Clients receive CFA state updates through regular block entity networking.
        return null;
    }
}
