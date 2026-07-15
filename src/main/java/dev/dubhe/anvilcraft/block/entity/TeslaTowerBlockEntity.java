package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.event.TeslaStrikeEvent;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.teslatower.HasCustomNameFilter;
import dev.dubhe.anvilcraft.api.teslatower.IsEntityIdFilter;
import dev.dubhe.anvilcraft.api.teslatower.IsFriendlyFilter;
import dev.dubhe.anvilcraft.api.teslatower.IsOnVehicleFilter;
import dev.dubhe.anvilcraft.api.teslatower.IsPetFilter;
import dev.dubhe.anvilcraft.api.teslatower.IsPlayerFilter;
import dev.dubhe.anvilcraft.api.teslatower.IsPlayerIdFilter;
import dev.dubhe.anvilcraft.api.teslatower.TeslaFilter;
import dev.dubhe.anvilcraft.block.power.consumer.TeslaTowerBlock;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import dev.dubhe.anvilcraft.util.DistanceComparator;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
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
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class TeslaTowerBlockEntity extends BlockEntity
    implements IPowerConsumer, MenuProvider, IDiskCloneable {
    private static final int STRIKE_COOLDOWN_TICKS = 4 * 20;
    private final ArrayList<Pair<TeslaFilter, String>> whiteList = new ArrayList<>();
    private int tickCount = STRIKE_COOLDOWN_TICKS;
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
        BlockState state = this.level.getBlockState(getBlockPos());
        return state.getValue(TeslaTowerBlock.HALF) == Vertical4PartHalf.BOTTOM
            && state.getValue(TeslaTowerBlock.SWITCH) == Switch.ON ? 128 : 0;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.targetEntityUUID != null) output.store("TargetEntityUUID", UUIDUtil.CODEC, this.targetEntityUUID);
        if (this.targetLightningRod != null) {
            output.putIntArray(
                "TargetLightningRod",
                new int[]{
                    this.targetLightningRod.getX(),
                    this.targetLightningRod.getY(),
                    this.targetLightningRod.getZ()
                }
            );
        }
        output.putLong("LastStrikeTime", this.lastStrikeTime);
        output.putInt("WhiteListSize", this.whiteList.size());
        for (int i = 0; i < this.whiteList.size(); i++) {
            Pair<TeslaFilter, String> entry = this.whiteList.get(i);
            output.putString("WhiteListId" + i, entry.first().getId());
            output.putString("WhiteListArg" + i, entry.second());
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.targetEntityUUID = input.read("TargetEntityUUID", UUIDUtil.CODEC).orElse(null);
        if (input.getIntArray("TargetLightningRod").isPresent()) {
            int[] arr = input.getIntArray("TargetLightningRod").orElse(new int[0]);
            this.targetLightningRod = new BlockPos(arr[0], arr[1], arr[2]);
        } else {
            this.targetLightningRod = null;
        }
        this.lastStrikeTime = input.getLongOr("LastStrikeTime", 0);
        this.whiteList.clear();
        int size = input.getIntOr("WhiteListSize", 0);
        for (int i = 0; i < size; i++) {
            String id = input.getStringOr("WhiteListId" + i, "");
            String arg = input.getStringOr("WhiteListArg" + i, "");
            this.whiteList.add(Pair.of(TeslaFilter.getFilter(id), arg));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putLong("LastStrikeTime", this.lastStrikeTime);
        if (this.targetEntityUUID != null) {
            tag.store("TargetEntityUUID", UUIDUtil.CODEC, this.targetEntityUUID);
        }
        if (this.targetLightningRod != null) {
            tag.putIntArray("TargetLightningRod", new int[]{
                this.targetLightningRod.getX(),
                this.targetLightningRod.getY(),
                this.targetLightningRod.getZ()
            });
        }
        tag.putInt("WhiteListSize", this.whiteList.size());
        for (int i = 0; i < this.whiteList.size(); i++) {
            Pair<TeslaFilter, String> entry = this.whiteList.get(i);
            tag.putString("WhiteListId" + i, entry.first().getId());
            tag.putString("WhiteListArg" + i, entry.second());
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        this.loadAdditional(input);
    }

    public void tick() {
        if (this.level == null) return;
        BlockState state = this.level.getBlockState(getBlockPos());
        if (!state.is(ModBlocks.TESLA_TOWER.get())) return;
        if (state.getValue(TeslaTowerBlock.HALF) != Vertical4PartHalf.BOTTOM) return;
        if (this.getGrid() == null) {
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
                this.level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
            }
        }
        if (!this.isGridWorking() || state.getValue(TeslaTowerBlock.SWITCH) == Switch.OFF) {
            this.tickCount = STRIKE_COOLDOWN_TICKS;
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
        this.tickCount = STRIKE_COOLDOWN_TICKS;
        this.tickCount--;
        AABB aabb = new AABB(this.getBlockPos().above(3)).expandTowards(8, 8, 8).expandTowards(-8, -8, -8);
        if (this.targetEntity != null) {
            if (!this.targetEntity.isAlive()) {
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
                LightningBolt lightningBolt = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
                lightningBolt.setPos(targetEntity.position());
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
            this.flashTimer = 5;
            this.level.playSound(null, getBlockPos(), ModSoundEvents.TESLA_TOWER_STRIKE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        } else {
            ArrayList<BlockPos> lightningRods = new ArrayList<>();
            BlockPos.betweenClosedStream(aabb)
                .forEach(it -> {
                    BlockState blockState = this.level.getBlockState(it);
                    if (blockState.is(ModBlockTags.LIGHTNING_RODS)) {
                        lightningRods.add(it.above(0));
                    }
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
            BlockState targetState = this.level.getBlockState(targetLightningRod);
            if (targetState.getBlock() instanceof LightningRodBlock rodBlock) {
                rodBlock.onLightningStrike(targetState, this.level, targetLightningRod);
            }
            this.flashTimer = 5;
            this.level.playSound(null, getBlockPos(), ModSoundEvents.TESLA_TOWER_STRIKE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private void clearTargetEntity(BlockState state) {
        this.targetEntity = null;
        this.targetEntityUUID = null;
        this.level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
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
            teslaTowerBlockEntity.setChanged();
        }
    }

    public void removeFilter(String id, String arg) {
        if (this.level == null) return;
        BlockState blockState = this.level.getBlockState(getBlockPos());
        int offsetY = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (this.level.getBlockEntity(getBlockPos().above(-offsetY)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            teslaTowerBlockEntity.whiteList.removeIf(pair -> pair.first().getId().equals(id) && pair.second().equals(arg));
            teslaTowerBlockEntity.setChanged();
        }
    }

    public void handleSync(List<Pair<TeslaFilter, String>> filters) {
        if (this.level == null) return;
        BlockState blockState = this.level.getBlockState(getBlockPos());
        int offsetY = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (this.level.getBlockEntity(getBlockPos().above(-offsetY)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            teslaTowerBlockEntity.whiteList.clear();
            teslaTowerBlockEntity.whiteList.addAll(filters);
            teslaTowerBlockEntity.setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.tesla_tower");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (this.level == null || player.isSpectator()) return null;
        BlockState blockState = this.level.getBlockState(getBlockPos());
        int offsetY = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (this.level.getBlockEntity(getBlockPos().above(-offsetY)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            return new TeslaTowerMenu(ModMenuTypes.TESLA_TOWER.get(), i, inventory, teslaTowerBlockEntity);
        }
        return null;
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
    public void storeDiskData(ValueOutput output) {
        ValueOutput.ValueOutputList filters = output.childrenList("filters");
        for (var entry : this.whiteList) {
            ValueOutput entryTag = filters.addChild();
            entryTag.putString("id", entry.first().getId());
            entryTag.putString("arg", entry.right());
        }
    }

    @Override
    public void applyDiskData(ValueInput input) {
        ValueInput.ValueInputList valueInputs = input.childrenListOrEmpty("filters");
        ArrayList<Pair<TeslaFilter, String>> filters = new ArrayList<>();
        valueInputs.forEach(it -> {
            Optional<String> id = it.getString("id");
            Optional<String> arg = it.getString("arg");
            if (id.isEmpty() || arg.isEmpty()) return;
            filters.add(Pair.of(TeslaFilter.getFilter(id.get()), arg.get()));
        });
        this.handleSync(filters);
    }
}
