package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.CelestialBackGateBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

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

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void configure(ResourceKey<Level> dimension, BlockPos portalPos, Direction facing) {
        boolean connectionChanged = !dimension.equals(returnDimension)
            || !portalPos.equals(returnPortalPos)
            || facing != returnFacing;
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
            syncWaterloggedState(serverLevel);
        }
    }

    @Nullable
    public ResourceKey<Level> getReturnDimension() {
        return returnDimension;
    }

    @Nullable
    public BlockPos getReturnPortalPos() {
        return returnPortalPos;
    }

    private void syncWaterloggedState(ServerLevel gateLevel) {
        if (returnDimension == null || returnPortalPos == null) return;
        ServerLevel returnLevel = gateLevel.getServer().getLevel(returnDimension);
        if (returnLevel == null || !returnLevel.hasChunkAt(returnPortalPos)) return;

        BlockState gateState = getBlockState();
        BlockState returnPortalState = returnLevel.getBlockState(returnPortalPos);
        if (!(gateState.getBlock() instanceof CelestialBackGateBlock)
            || !(returnPortalState.getBlock() instanceof CelestialForgingAnvilPortalBlock)) {
            return;
        }

        boolean gateWaterlogged = gateState.getValue(BlockStateProperties.WATERLOGGED);
        boolean returnPortalWaterlogged = returnPortalState.getValue(BlockStateProperties.WATERLOGGED);
        if (!waterloggedStateInitialized) {
            boolean waterlogged = gateWaterlogged || returnPortalWaterlogged;
            setWaterlogged(gateLevel, worldPosition, gateState, waterlogged);
            setWaterlogged(returnLevel, returnPortalPos, returnPortalState, waterlogged);
            updateWaterloggedState(waterlogged, waterlogged);
            return;
        }

        boolean gateChanged = gateWaterlogged != lastWaterlogged;
        boolean returnPortalChanged = returnPortalWaterlogged != lastReturnPortalWaterlogged;
        if (gateChanged && !returnPortalChanged) {
            setWaterlogged(returnLevel, returnPortalPos, returnPortalState, gateWaterlogged);
            returnPortalWaterlogged = gateWaterlogged;
        } else if (!gateChanged && returnPortalChanged) {
            setWaterlogged(gateLevel, worldPosition, gateState, returnPortalWaterlogged);
            gateWaterlogged = returnPortalWaterlogged;
        } else if (gateChanged && returnPortalChanged && gateWaterlogged != returnPortalWaterlogged) {
            boolean waterlogged = gateWaterlogged || returnPortalWaterlogged;
            setWaterlogged(gateLevel, worldPosition, gateState, waterlogged);
            setWaterlogged(returnLevel, returnPortalPos, returnPortalState, waterlogged);
            gateWaterlogged = waterlogged;
            returnPortalWaterlogged = waterlogged;
        }
        updateWaterloggedState(gateWaterlogged, returnPortalWaterlogged);
    }

    private static void setWaterlogged(ServerLevel level, BlockPos pos, BlockState state, boolean waterlogged) {
        if (state.getValue(BlockStateProperties.WATERLOGGED) == waterlogged) return;
        level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, waterlogged), 3);
        if (waterlogged) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }

    private void updateWaterloggedState(boolean gateWaterlogged, boolean returnPortalWaterlogged) {
        if (waterloggedStateInitialized
            && lastWaterlogged == gateWaterlogged
            && lastReturnPortalWaterlogged == returnPortalWaterlogged) {
            return;
        }
        this.lastWaterlogged = gateWaterlogged;
        this.lastReturnPortalWaterlogged = returnPortalWaterlogged;
        this.waterloggedStateInitialized = true;
        setChanged();
    }

    public void tick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        syncWaterloggedState(serverLevel);
        if (!duplicateCleanupDone && returnDimension != null && returnPortalPos != null) {
            CelestialTravelManager.cleanupDuplicateGates(
                serverLevel, worldPosition, returnDimension, returnPortalPos, returnFacing
            );
            duplicateCleanupDone = true;
        }
        AABB gateBox = new AABB(worldPosition);
        for (UUID uuid : Set.copyOf(pendingEntities)) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity != null && gateBox.intersects(entity.getBoundingBox())) tryTouch(entity);
        }
        pendingEntities.clear();
        for (Entity entity : serverLevel.getEntitiesOfClass(Entity.class, gateBox)) {
            tryTouch(entity);
        }
        if (!touchingEntities.isEmpty()) {
            Set<UUID> present = serverLevel.getEntitiesOfClass(Entity.class, gateBox)
                .stream().map(Entity::getUUID).collect(Collectors.toSet());
            touchingEntities.removeIf(uuid -> !present.contains(uuid));
        }
    }

    public void tryTouch(Entity entity) {
        UUID uuid = entity.getUUID();
        if (touchingEntities.contains(uuid)) return;
        if (CelestialTravelManager.tryReturn(entity, this)) {
            touchingEntities.add(uuid);
        }
    }

    /** Queue players for the next block-entity tick so movement packets are fully processed first. */
    public void queueTouch(Entity entity) {
        pendingEntities.add(entity.getUUID());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (returnDimension != null) {
            tag.putString("returnDimension", returnDimension.location().toString());
        }
        if (returnPortalPos != null) {
            tag.putLong("returnPortalPos", returnPortalPos.asLong());
        }
        tag.putString("returnFacing", returnFacing.getName());
        tag.putBoolean("lastWaterlogged", lastWaterlogged);
        tag.putBoolean("lastReturnPortalWaterlogged", lastReturnPortalWaterlogged);
        tag.putBoolean("waterloggedStateInitialized", waterloggedStateInitialized);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String dimensionName = tag.getString("returnDimension");
        if (!dimensionName.isEmpty()) {
            try {
                returnDimension = ResourceKey.create(
                    Registries.DIMENSION, ResourceLocation.parse(dimensionName)
                );
            } catch (IllegalArgumentException ignored) {
                returnDimension = null;
            }
        } else {
            returnDimension = null;
        }
        returnPortalPos = tag.contains("returnPortalPos")
            ? BlockPos.of(tag.getLong("returnPortalPos")) : null;
        Direction parsedFacing = Direction.byName(tag.getString("returnFacing"));
        returnFacing = parsedFacing != null && parsedFacing.getAxis().isHorizontal()
            ? parsedFacing : Direction.NORTH;
        duplicateCleanupDone = false;
        lastWaterlogged = tag.getBoolean("lastWaterlogged");
        lastReturnPortalWaterlogged = tag.getBoolean("lastReturnPortalWaterlogged");
        waterloggedStateInitialized = tag.getBoolean("waterloggedStateInitialized");
    }
}
