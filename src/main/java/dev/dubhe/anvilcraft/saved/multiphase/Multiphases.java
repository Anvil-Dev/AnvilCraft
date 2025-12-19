package dev.dubhe.anvilcraft.saved.multiphase;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import dev.dubhe.anvilcraft.saved.BetterSavedData;
import dev.dubhe.anvilcraft.saved.datafixers.DataFixers;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.recover.RecoverEntry;
import dev.dubhe.anvilcraft.util.recover.RecoverStation;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter(AccessLevel.PRIVATE)
public class Multiphases extends BetterSavedData {
    public static final Codec<Multiphases> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Codec.unboundedMap(UUIDUtil.CODEC, Multiphase.CODEC.codec())
            .fieldOf("multiphases")
            .forGetter(Multiphases::getMultiphases),
        RecoverStation.codec(Multiphase.CODEC)
            .forGetter(Multiphases::getRecover)
    ).apply(ins, Multiphases::new));
    private static final Multiphases CLIENT_COPY = new Multiphases();
    private static final ResourceLocation FIXER_ID = AnvilCraft.of("multiphases_fixers");
    private static final double CURRENT_VERSION = 0.0;
    private final Map<UUID, Multiphase> multiphases;
    private final RecoverStation<Multiphase> recover;

    private Multiphases() {
        this.multiphases = new HashMap<>();
        this.recover = RecoverStation.create(AnvilCraft.CONFIG.multiphaseRecoverMaxSize);
    }

    private Multiphases(Map<UUID, Multiphase> multiphases, RecoverStation<Multiphase> recover) {
        this.multiphases = new HashMap<>(multiphases);
        this.recover = recover;
    }

    public static Multiphases get() {
        return Multiphases.get("multiphases", Multiphases::new, Multiphases.CLIENT_COPY);
    }

    public Optional<Multiphase> get(@NotNull UUID id) {
        return Optional.ofNullable(this.multiphases.get(id));
    }

    public Multiphase getOrCreate(@NotNull UUID id) {
        return this.multiphases.computeIfAbsent(id, id1 -> Multiphase.EMPTY);
    }

    public void put(@NotNull UUID id, @NotNull Multiphase multiphase) {
        this.multiphases.put(id, multiphase);
        this.sync2C();
        this.setDirty();
    }

    public UUID put(@NotNull Multiphase multiphase) {
        var id = Multiphases.generate(this.multiphases::containsKey);
        this.multiphases.put(id, multiphase);
        this.sync2C();
        this.setDirty();
        return id;
    }

    public void discard(UUID id, RegistryAccess registries) {
        this.multiphases.remove(id);
        this.sync2C(registries);
        this.setDirty();
    }

    public boolean remove(UUID id, RegistryAccess registries) {
        Multiphase removed = this.multiphases.remove(id);
        if (removed != null) {
            this.recover.removed(id, removed);
            this.sync2C(registries);
        }
        this.setDirty();
        return removed != null;
    }

    public boolean contains(UUID id) {
        return this.multiphases.containsKey(id);
    }

    @Override
    protected void registerDataFixers() {
        DataFixers.registerFixer(FIXER_ID);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries) {
        this.multiphases.clear();
        for (Tag tag : nbt.getList("Multiphases", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag multiphaseNbt)) throw new IllegalStateException("'" + tag + "' is not a valid compound tag!");
            multiphaseNbt = DataFixers.fixData(FIXER_ID, CURRENT_VERSION, multiphaseNbt, registries);
            var id = multiphaseNbt.getUUID("id");
            Multiphase.CODEC.compressedDecode(registries.createSerializationContext(NbtOps.INSTANCE), multiphaseNbt.get("content"))
                .ifSuccess(multiphase -> this.multiphases.put(id, multiphase));
        }
        for (Tag tag : nbt.getList("Recovers", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag multiphaseNbt)) throw new IllegalStateException("'" + tag + "' is not a valid compound tag!");
            multiphaseNbt = DataFixers.fixData(FIXER_ID, CURRENT_VERSION, multiphaseNbt, registries);
            var id = multiphaseNbt.getUUID("id");
            Multiphase.CODEC.compressedDecode(registries.createSerializationContext(NbtOps.INSTANCE), multiphaseNbt.get("content"))
                .ifSuccess(multiphase -> this.multiphases.put(id, multiphase));
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag multiphases = new ListTag();
        for (UUID id : this.multiphases.keySet()) {
            CompoundTag multiphase = new CompoundTag();
            multiphase.putUUID("id", id);
            multiphase.put(
                "content",
                CodecUtil.encodeStart(
                    Multiphase.CODEC,
                    registries.createSerializationContext(NbtOps.INSTANCE),
                    this.multiphases.get(id)
                ).getOrThrow()
            );
            multiphases.add(multiphase);
        }
        nbt.put("Multiphases", multiphases);

        ListTag recovers = new ListTag();
        for (var entry : this.recover.getEntries()) {
            CompoundTag recover = new CompoundTag();
            recover.putUUID("id", entry.id());
            recover.put(
                "content",
                CodecUtil.encodeStart(
                    Multiphase.CODEC,
                    registries.createSerializationContext(NbtOps.INSTANCE),
                    entry.value()
                ).getOrThrow()
            );
            recovers.add(recover);
        }
        nbt.put("Recovers", recovers);

        return nbt;
    }

    @Override
    protected Packet<MultiphasePackets.AllSync> createPacket(RegistryAccess registryAccess) {
        return new Packet<>(
            MultiphasePackets.AllSync.TYPE,
            MultiphasePackets.AllSync.STREAM_CODEC,
            new MultiphasePackets.AllSync(this.multiphases, this.recover.recoverableIds())
        );
    }

    public void sync(Map<UUID, Multiphase> multiphases, Set<UUID> recoverableIds) {
        this.multiphases.clear();
        this.multiphases.putAll(multiphases);
        this.recover.sync(true, recoverableIds);
    }

    public Set<UUID> getIDs() {
        return this.multiphases.keySet();
    }

    public Set<UUID> getRecoverableIDs() {
        return this.recover.recoverableIds();
    }

    // 命令

    public boolean recover(UUID id, RegistryAccess registries) {
        var recoveredOp = this.recover.recover(id);
        if (recoveredOp.isEmpty()) return false;
        RecoverEntry<Multiphase> recovered = recoveredOp.get();
        this.multiphases.put(recovered.id(), recovered.value());
        this.sync2C(registries);
        this.setDirty();
        return true;
    }

    public void clearRecoverFromCommand() {
        this.clearRecover();
        this.setDirty();
        PacketDistributor.sendToAllPlayers(new MultiphasePackets.RecoverClear());
    }

    public void clearRecover() {
        this.recover.clear();
    }
}
