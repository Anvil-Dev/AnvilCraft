package dev.dubhe.anvilcraft.saved.multiphase;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import dev.dubhe.anvilcraft.saved.BetterSavedData;
import dev.dubhe.anvilcraft.saved.datafixer.DataFixers;
import dev.dubhe.anvilcraft.saved.multiphase.fixer.V0_1;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.Util;
import dev.dubhe.anvilcraft.util.recover.RecoverEntry;
import dev.dubhe.anvilcraft.util.recover.RecoverStation;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

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
    private static final double CURRENT_VERSION = 0.1;
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

    public Optional<Multiphase> get(UUID id) {
        return Optional.ofNullable(this.multiphases.get(id));
    }

    public Multiphase getOrCreate(UUID id) {
        return this.multiphases.computeIfAbsent(id, id1 -> Multiphase.EMPTY);
    }

    public void put(UUID id, Multiphase multiphase) {
        this.multiphases.put(id, multiphase);
        this.sync2C();
        this.setDirty();
    }

    public UUID put(Multiphase multiphase) {
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
        DataFixers.registerFixer(FIXER_ID, new V0_1());
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt = DataFixers.fixData(FIXER_ID, CURRENT_VERSION, nbt.getDouble("version"), nbt, registries);
        this.multiphases.clear();
        CompoundTag multiphases = nbt.getCompound("multiphases");
        for (String rawId : multiphases.getAllKeys()) {
            Multiphase.CODEC.decoder()
                .decode(registries.createSerializationContext(NbtOps.INSTANCE), multiphases.get(rawId))
                .map(Pair::getFirst)
                .ifSuccess(multiphase -> this.recover.getEntries().add(new RecoverEntry<>(UUID.fromString(rawId), multiphase)));
        }
        CompoundTag recover = nbt.getCompound("recover");
        for (String rawId : recover.getAllKeys()) {
            Multiphase.CODEC.decoder()
                .decode(registries.createSerializationContext(NbtOps.INSTANCE), recover.get(rawId))
                .map(Pair::getFirst)
                .ifSuccess(multiphase -> this.recover.getEntries().add(new RecoverEntry<>(UUID.fromString(rawId), multiphase)));
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putDouble("version", CURRENT_VERSION);

        CompoundTag multiphases = new CompoundTag();
        for (UUID id : this.multiphases.keySet()) {
            CodecUtil.encode(
                Multiphase.CODEC.fieldOf(id.toString()),
                this.multiphases.get(id),
                registries.createSerializationContext(NbtOps.INSTANCE),
                multiphases
            ).map(Util::<CompoundTag>cast).ifSuccess(multiphases::merge);
        }
        nbt.put("multiphases", multiphases);

        CompoundTag recover = new CompoundTag();
        for (var entry : this.recover.getEntries()) {
            CodecUtil.encode(
                Multiphase.CODEC.fieldOf(entry.id().toString()),
                entry.value(),
                registries.createSerializationContext(NbtOps.INSTANCE),
                recover
            ).map(Util::<CompoundTag>cast).ifSuccess(recover::merge);
        }
        nbt.put("recover", recover);

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
        this.recover.sync(recoverableIds);
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