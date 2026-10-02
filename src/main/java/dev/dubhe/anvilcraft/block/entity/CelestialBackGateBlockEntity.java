package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.CelestialBackGateBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Stores the source portal to which a generated return gate leads. */
public class CelestialBackGateBlockEntity extends BlockEntity {
    @Nullable
    private ResourceKey<Level> returnDimension;
    @Nullable
    private BlockPos returnPortalPos;
    @Getter
    private Direction returnFacing = Direction.NORTH;
    private final Set<UUID> touchingEntities = new HashSet<>();
    private final Set<UUID> pendingEntities = new HashSet<>();
    private boolean duplicateCleanupDone;
    private boolean lastWaterlogged;
    private boolean lastReturnPortalWaterlogged;
    private boolean waterloggedStateInitialized;

    public CelestialBackGateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void configure(ResourceKey<Level> dimension, BlockPos portalPos, Direction facing) {
        final boolean connectionChanged = !dimension.equals(this.returnDimension)
            || !portalPos.equals(this.returnPortalPos)
            || facing != this.returnFacing;
        this.returnDimension = dimension;
        this.returnPortalPos = portalPos.immutable();
        this.returnFacing = facing;
        this.duplicateCleanupDone = false;
        if (connectionChanged) {
            this.waterloggedStateInitialized = false;
        }
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (level instanceof ServerLevel serverLevel) {
            this.syncWaterloggedState(serverLevel);
        }
    }

    @Nullable
    public ResourceKey<Level> getReturnDimension() {
        return this.returnDimension;
    }

    @Nullable
    public BlockPos getReturnPortalPos() {
        return this.returnPortalPos;
    }

    private void syncWaterloggedState(ServerLevel gateLevel) {
        if (this.returnDimension == null || this.returnPortalPos == null) return;
        ServerLevel returnLevel = gateLevel.getServer().getLevel(this.returnDimension);
        if (returnLevel == null || !returnLevel.hasChunkAt(this.returnPortalPos)) return;

        BlockState gateState = getBlockState();
        BlockState returnPortalState = returnLevel.getBlockState(this.returnPortalPos);
        if (!(gateState.getBlock() instanceof CelestialBackGateBlock)
            || !(returnPortalState.getBlock() instanceof CelestialForgingAnvilPortalBlock)) {
            return;
        }

        boolean gateWaterlogged = gateState.getValue(BlockStateProperties.WATERLOGGED);
        boolean returnPortalWaterlogged = returnPortalState.getValue(BlockStateProperties.WATERLOGGED);
        if (!this.waterloggedStateInitialized) {
            boolean waterlogged = gateWaterlogged || returnPortalWaterlogged;
            setWaterlogged(gateLevel, worldPosition, gateState, waterlogged);
            setWaterlogged(returnLevel, this.returnPortalPos, returnPortalState, waterlogged);
            this.updateWaterloggedState(waterlogged, waterlogged);
            return;
        }

        boolean gateChanged = gateWaterlogged != this.lastWaterlogged;
        boolean returnPortalChanged = returnPortalWaterlogged != this.lastReturnPortalWaterlogged;
        if (gateChanged && !returnPortalChanged) {
            setWaterlogged(returnLevel, this.returnPortalPos, returnPortalState, gateWaterlogged);
            returnPortalWaterlogged = gateWaterlogged;
        } else if (!gateChanged && returnPortalChanged) {
            setWaterlogged(gateLevel, worldPosition, gateState, returnPortalWaterlogged);
            gateWaterlogged = returnPortalWaterlogged;
        } else if (gateChanged && returnPortalChanged && gateWaterlogged != returnPortalWaterlogged) {
            boolean waterlogged = gateWaterlogged || returnPortalWaterlogged;
            setWaterlogged(gateLevel, worldPosition, gateState, waterlogged);
            setWaterlogged(returnLevel, this.returnPortalPos, returnPortalState, waterlogged);
            gateWaterlogged = waterlogged;
            returnPortalWaterlogged = waterlogged;
        }
        this.updateWaterloggedState(gateWaterlogged, returnPortalWaterlogged);
    }

    private static void setWaterlogged(ServerLevel level, BlockPos pos, BlockState state, boolean waterlogged) {
        if (state.getValue(BlockStateProperties.WATERLOGGED) == waterlogged) return;
        level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, waterlogged), 3);
        if (waterlogged) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }

    private void updateWaterloggedState(boolean gateWaterlogged, boolean returnPortalWaterlogged) {
        if (this.waterloggedStateInitialized
            && this.lastWaterlogged == gateWaterlogged
            && this.lastReturnPortalWaterlogged == returnPortalWaterlogged) {
            return;
        }
        this.lastWaterlogged = gateWaterlogged;
        this.lastReturnPortalWaterlogged = returnPortalWaterlogged;
        this.waterloggedStateInitialized = true;
        setChanged();
    }

    public void tick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        this.syncWaterloggedState(serverLevel);
        if (!this.duplicateCleanupDone && this.returnDimension != null && this.returnPortalPos != null) {
            CelestialTravelManager.cleanupDuplicateGates(
                serverLevel, worldPosition, this.returnDimension, this.returnPortalPos, this.returnFacing
            );
            this.duplicateCleanupDone = true;
        }
        AABB gateBox = new AABB(worldPosition);
        for (UUID uuid : Set.copyOf(this.pendingEntities)) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity != null && gateBox.intersects(entity.getBoundingBox())) this.tryTouch(entity);
        }
        this.pendingEntities.clear();
        for (Entity entity : serverLevel.getEntitiesOfClass(Entity.class, gateBox)) {
            this.tryTouch(entity);
        }
        if (!this.touchingEntities.isEmpty()) {
            Set<UUID> present = serverLevel.getEntitiesOfClass(Entity.class, gateBox)
                .stream().map(Entity::getUUID).collect(Collectors.toSet());
            this.touchingEntities.removeIf(uuid -> !present.contains(uuid));
        }
    }

    public void tryTouch(Entity entity) {
        UUID uuid = entity.getUUID();
        if (this.touchingEntities.contains(uuid)) return;
        if (CelestialTravelManager.tryReturn(entity, this)) {
            this.touchingEntities.add(uuid);
        }
    }

    /** Queue players for the next block-entity tick so movement packets are fully processed first. */
    public void queueTouch(Entity entity) {
        this.pendingEntities.add(entity.getUUID());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        if (this.returnDimension != null) {
            tag.putString("returnDimension", this.returnDimension.identifier().toString());
        }
        if (this.returnPortalPos != null) {
            tag.putLong("returnPortalPos", this.returnPortalPos.asLong());
        }
        tag.putString("returnFacing", this.returnFacing.getName());
        tag.putBoolean("lastWaterlogged", this.lastWaterlogged);
        tag.putBoolean("lastReturnPortalWaterlogged", this.lastReturnPortalWaterlogged);
        tag.putBoolean("waterloggedStateInitialized", this.waterloggedStateInitialized);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
        Identifier dimension = Identifier.tryParse(tag.getStringOr("returnDimension", ""));
        this.returnDimension = dimension == null ? null : ResourceKey.create(Registries.DIMENSION, dimension);
        this.returnPortalPos = tag.contains("returnPortalPos")
            ? BlockPos.of(tag.getLongOr("returnPortalPos", 0L)) : null;
        Direction parsedFacing = Direction.byName(tag.getStringOr("returnFacing", ""));
        this.returnFacing = parsedFacing != null && parsedFacing.getAxis().isHorizontal()
            ? parsedFacing : Direction.NORTH;
        this.duplicateCleanupDone = false;
        this.lastWaterlogged = tag.getBooleanOr("lastWaterlogged", false);
        this.lastReturnPortalWaterlogged = tag.getBooleanOr("lastReturnPortalWaterlogged", false);
        this.waterloggedStateInitialized = tag.getBooleanOr("waterloggedStateInitialized", false);
    }
}
