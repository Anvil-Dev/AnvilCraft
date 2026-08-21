package dev.dubhe.anvilcraft.block.entity;

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

    public CelestialBackGateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void configure(ResourceKey<Level> dimension, BlockPos portalPos, Direction facing) {
        this.returnDimension = dimension;
        this.returnPortalPos = portalPos.immutable();
        this.returnFacing = facing;
        this.duplicateCleanupDone = false;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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

    public void tick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
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
    }
}
