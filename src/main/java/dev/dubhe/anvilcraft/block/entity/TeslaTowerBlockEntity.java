package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.event.TeslaStrikeEvent;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.taslatower.HasCustomNameFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsEntityIdFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsFriendlyFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsOnVehicleFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsPetFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsPlayerFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsPlayerIdFilter;
import dev.dubhe.anvilcraft.api.taslatower.TeslaFilter;
import dev.dubhe.anvilcraft.block.TeslaTowerBlock;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import dev.dubhe.anvilcraft.util.DistanceComparator;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

@Slf4j
public class TeslaTowerBlockEntity extends BlockEntity
    implements IPowerConsumer, MenuProvider, IDiskCloneable {
    private final ArrayList<Pair<TeslaFilter, String>> whiteList = new ArrayList<>();
    private int tickCount = 0;
    private int flashTimer = 0;
    @Getter
    private long lastStrikeTime = 0;
    @Setter
    @Getter
    private @Nullable PowerGrid grid;
    private @Nullable LivingEntity targetEntity;
    @Getter
    private @Nullable UUID targetEntityUUID;
    @Getter
    private @Nullable BlockPos targetLightningRod;

    public TeslaTowerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TESLA_TOWER.get(), pos, blockState);
    }

    private TeslaTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static TeslaTowerBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new TeslaTowerBlockEntity(type, pos, blockState);
    }

    @Override
    public PowerComponentType getComponentType() {
        if (this.getLevel() == null) return PowerComponentType.INVALID;
        if (!this.getBlockState().is(ModBlocks.TESLA_TOWER.get())) return PowerComponentType.INVALID;
        if (this.getBlockState().getValue(TeslaTowerBlock.HALF) != Vertical4PartHalf.BOTTOM) return PowerComponentType.INVALID;
        return PowerComponentType.CONSUMER;
    }

    @Override
    public int getInputPower() {
        if (this.level == null) return 0;
        return this.level.getBlockState(getBlockPos()).getValue(TeslaTowerBlock.HALF) == Vertical4PartHalf.BOTTOM ? 128 : 0;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("LastStrikeTime", this.lastStrikeTime);
        if (this.targetEntityUUID != null) {
            tag.putUUID("TargetEntityUUID", this.targetEntityUUID);
        }
        if (this.targetLightningRod != null) {
            tag.putIntArray(
                "TargetLightningRod",
                new int[]{
                    this.targetLightningRod.getX(),
                    this.targetLightningRod.getY(),
                    this.targetLightningRod.getZ()
                }
            );
        }
        int index = 0;
        for (Pair<TeslaFilter, String> entry : this.whiteList) {
            tag.putString(entry.first().getId() + "_-_" + index, entry.second());
            index++;
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.lastStrikeTime = tag.getLong("LastStrikeTime");
        if (tag.contains("TargetEntityUUID")) {
            this.targetEntityUUID = tag.getUUID("TargetEntityUUID");
        } else {
            this.targetEntityUUID = null;
        }
        if (tag.contains("TargetLightningRod")) {
            int[] arr = tag.getIntArray("TargetLightningRod");
            this.targetLightningRod = new BlockPos(arr[0], arr[1], arr[2]);
        } else {
            this.targetLightningRod = null;
        }
        this.whiteList.clear();
        for (String key : tag.getAllKeys()) {
            if (key.split("_-_").length != 2) continue;
            String id = key.split("_-_")[0];
            this.whiteList.add(Pair.of(TeslaFilter.getFilter(id), tag.getString(key)));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void tick() {
        if (this.level == null) return;
        BlockState state = this.level.getBlockState(getBlockPos());
        if (!state.is(ModBlocks.TESLA_TOWER.get())) return;
        if (state.getValue(TeslaTowerBlock.HALF) != Vertical4PartHalf.BOTTOM) return;
        if (state.getValue(TeslaTowerBlock.SWITCH) == Switch.OFF && this.getGrid() != null) {
            this.getGrid().remove(this);
        } else if (state.getValue(TeslaTowerBlock.SWITCH) == Switch.ON && this.getGrid() == null) {
            PowerGrid.addComponent(this);
        }
        if (this.getComponentType() == PowerComponentType.INVALID) {
            this.targetEntity = null;
            this.targetEntityUUID = null;
            this.targetLightningRod = null;
        }
        this.flushState(this.level, getBlockPos());
        this.flushState(this.level, getBlockPos().above(1));
        this.flushState(this.level, getBlockPos().above(2));
        this.flushState(this.level, getBlockPos().above(3));
        if (this.level.isClientSide()) return;
        if (this.flashTimer > 0) {
            this.flashTimer--;
            if (this.flashTimer == 0) {
                this.targetEntity = null;
                this.targetEntityUUID = null;
                this.targetLightningRod = null;
                this.setChanged();
                this.level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
            }
        }
        if (state.getValue(TeslaTowerBlock.OVERLOAD) || state.getValue(TeslaTowerBlock.SWITCH) == Switch.OFF) {
            final boolean hasChanged = this.targetEntity != null || this.targetEntityUUID != null || this.targetLightningRod != null;
            this.targetEntity = null;
            this.targetEntityUUID = null;
            this.targetLightningRod = null;
            this.flashTimer = 0;
            if (hasChanged) {
                this.setChanged();
                this.level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
            }
            return;
        }
        if (this.tickCount > 0) {
            this.tickCount--;
            return;
        }
        this.tickCount = 80;
        this.tickCount--;
        AABB aabb = new AABB(this.getBlockPos().above(3)).expandTowards(8, 8, 8).expandTowards(-8, -8, -8);
        if (this.targetEntity != null) {
            if (!targetEntity.isAlive()) {
                this.clearTargetEntity(state);
            } else {
                AABB boundingBox = this.targetEntity.getBoundingBox();
                if (!aabb.intersects(boundingBox)) {
                    this.clearTargetEntity(state);
                }
            }
        }
        Optional<LivingEntity> target = this.level.getEntitiesOfClass(LivingEntity.class, aabb)
            .stream()
            .filter(LivingEntity::isAlive)
            .filter(it -> this.whiteList.stream().noneMatch(it2 -> it2.left().match(it, it2.right())))
            .min((e1, e2) -> new DistanceComparator(getBlockPos().getCenter()).compare(e1.position(), e2.position()));
        if (target.isPresent()) {
            LivingEntity targetEntity = target.get();
            if (NeoForge.EVENT_BUS.post(new TeslaStrikeEvent.TargetEntity(this.level, this, targetEntity)).isCanceled()) {
                this.clearTargetEntity(state);
                return;
            }
            this.targetEntity = targetEntity;
            this.targetEntityUUID = targetEntity.getUUID();
            this.lastStrikeTime = this.level.getGameTime();
            this.level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
            if (this.level instanceof ServerLevel serverLevel) {
                LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
                if (lightningBolt != null) {
                    lightningBolt.moveTo(targetEntity.position());
                    lightningBolt.setDamage(lightningBolt.getDamage() * 2);
                    if (!EventHooks.onEntityStruckByLightning(targetEntity, lightningBolt)) {
                        targetEntity.thunderHit(serverLevel, lightningBolt);
                    }
                    if (!targetEntity.isAlive() || targetEntity.isRemoved()) {
                        AABB area = new AABB(targetEntity.blockPosition()).inflate(1.0);
                        LivingEntity converted = this.level.getEntitiesOfClass(LivingEntity.class, area,
                            e -> e != targetEntity && e.isAlive()).stream().findFirst().orElse(targetEntity);
                        this.targetEntity = converted;
                        this.targetEntityUUID = converted.getUUID();
                        this.level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
                    }
                }
            }
            this.flashTimer = 5;
            this.level.playSound(null, getBlockPos(), ModSoundEvents.TESLA_TOWER_STRIKE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        } else {
            ArrayList<BlockPos> lightningRods = new ArrayList<>();
            BlockPos.betweenClosedStream(aabb)
                .forEach(it -> {
                    if (this.level.getBlockState(it).is(Blocks.LIGHTNING_ROD)) lightningRods.add(it.above(0));
                });
            Optional<BlockPos> targetBlock = lightningRods.stream()
                .min((b1, b2) -> new DistanceComparator(getBlockPos().getCenter()).compare(b1.getCenter(), b2.getCenter()));
            if (targetBlock.isEmpty()) return;
            BlockPos targetLightningRod = targetBlock.get();
            if (NeoForge.EVENT_BUS.post(new TeslaStrikeEvent.TargetBlock(this.level, this, targetLightningRod)).isCanceled()) {
                this.targetLightningRod = null;
                this.level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
                return;
            }
            this.targetLightningRod = targetLightningRod;
            this.lastStrikeTime = this.level.getGameTime();
            this.level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
            ((LightningRodBlock) Blocks.LIGHTNING_ROD).onLightningStrike(
                this.level.getBlockState(targetLightningRod),
                this.level,
                targetLightningRod
            );
            this.flashTimer = 5;
            this.level.playSound(null, getBlockPos(), ModSoundEvents.TESLA_TOWER_STRIKE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private void clearTargetEntity(BlockState state) {
        this.targetEntity = null;
        this.targetEntityUUID = null;
        Objects.requireNonNull(this.level).sendBlockUpdated(this.getBlockPos(), state, state, 2);
    }

    public void initWhiteList(Player player) {
        this.whiteList.add(Pair.of(new IsPlayerFilter(), ""));
        this.whiteList.add(Pair.of(new IsPlayerIdFilter(), player.getName().getString()));
        this.whiteList.add(Pair.of(new IsPetFilter(), ""));
        this.whiteList.add(Pair.of(new HasCustomNameFilter(), ""));
        this.whiteList.add(Pair.of(new IsEntityIdFilter(), "minecraft:villager"));
        this.whiteList.add(Pair.of(new IsEntityIdFilter(), "minecraft:wandering_trader"));
        this.whiteList.add(Pair.of(new IsFriendlyFilter(), ""));
        this.whiteList.add(Pair.of(new IsOnVehicleFilter(), ""));
    }

    public void addFilter(String id, String arg) {
        if (this.level == null) return;
        BlockState blockState = this.level.getBlockState(getBlockPos());
        int offsetY = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (this.level.getBlockEntity(getBlockPos().above(-offsetY)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            teslaTowerBlockEntity.whiteList.add(Pair.of(TeslaFilter.getFilter(id), arg));
        }
    }

    public void removeFilter(String id, String arg) {
        if (this.level == null) return;
        BlockState blockState = this.level.getBlockState(getBlockPos());
        int offsetY = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (this.level.getBlockEntity(getBlockPos().above(-offsetY)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            teslaTowerBlockEntity.whiteList.removeIf(pair -> pair.first().getId().equals(id) && pair.second().equals(arg));
        }
    }

    public void handleSync(List<Pair<TeslaFilter, String>> filters) {
        if (this.level == null) return;
        BlockState blockState = this.level.getBlockState(getBlockPos());
        int offsetY = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (this.level.getBlockEntity(getBlockPos().above(-offsetY)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            teslaTowerBlockEntity.whiteList.clear();
            teslaTowerBlockEntity.whiteList.addAll(filters);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.tesla_tower");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (this.level == null) return null;
        BlockState blockState = this.level.getBlockState(getBlockPos());
        int offsetY = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (this.level.getBlockEntity(getBlockPos().above(-offsetY)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            return new TeslaTowerMenu(ModMenuTypes.TESLA_TOWER.get(), i, inventory, teslaTowerBlockEntity);
        }
        return null;
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
    }

    public List<Pair<TeslaFilter, String>> getWhiteList() {
        if (this.level == null) return List.of();
        BlockState blockState = this.level.getBlockState(getBlockPos());
        int offsetY = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (this.level.getBlockEntity(getBlockPos().above(-offsetY)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            return teslaTowerBlockEntity.whiteList;
        }
        return List.of();
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        ListTag filters = new ListTag();
        for (var entry : this.whiteList) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("id", entry.first().getId());
            entryTag.putString("arg", entry.right());
            filters.add(entryTag);
        }
        tag.put("Filters", filters);
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        ArrayList<Pair<TeslaFilter, String>> filters = new ArrayList<>();
        for (Tag tag : data.getList("Filters", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag filter)) continue;
            filters.add(Pair.of(TeslaFilter.getFilter(filter.getString("id")), filter.getString("arg")));
        }
        this.handleSync(filters);
    }
}
