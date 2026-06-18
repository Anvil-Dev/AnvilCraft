package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.saved.WormholeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CelestialForgingAnvilPortalBlockEntity extends BlockEntity {

    /**
     * Set of entity UUIDs currently touching this portal.
     * Used to track enter/leave so that teleport fires once per touch,
     * and resets when the entity steps away.
     */
    private final Set<UUID> touchingEntities = new HashSet<>();

    public CelestialForgingAnvilPortalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * Find the parent CFA block entity that this portal is attached to.
     */
    @Nullable
    public CelestialForgingAnvilBlockEntity findParentCfa() {
        if (level == null) return null;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CelestialForgingAnvilPortalBlock)) return null;

        // FACING points away from CFA; look opposite to find CFA
        Direction towardsCfa = state.getValue(CelestialForgingAnvilPortalBlock.FACING).getOpposite();
        BlockPos cfaPos = worldPosition.relative(towardsCfa);
        BlockState cfaState = level.getBlockState(cfaPos);
        if (cfaState.getBlock() instanceof CelestialForgingAnvilBlock) {
            Cube323PartHalf half = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
            BlockPos controllerPos = cfaPos.offset(half.getOffset().multiply(-1));
            if (level.getBlockEntity(controllerPos) instanceof CelestialForgingAnvilBlockEntity cfaBe) {
                return cfaBe;
            }
        }
        return null;
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        CelestialForgingAnvilBlockEntity parent = findParentCfa();
        if (parent == null) return;
        Cube323PartHalf side = findSideFromParent(parent);
        if (side == null) return;

        // Query wormhole network directly to determine if portal should be open
        WormholeNetwork network = WormholeNetwork.get();
        int hash = parent.getWormholeParamsHash();
        List<WormholeNetwork.Entry> connected = network.getConnected(
            hash, level.dimension(), parent.getBlockPos()
        );
        int groupSize = network.getGroupSize(hash);
        boolean hasMatching = connected.stream()
            .anyMatch(e -> network.hasPortalAt(e.dimension(), e.pos(), side));
        boolean shouldBeOpen = groupSize == 2 && hasMatching;

        BlockState state = getBlockState();
        if (state.getBlock() instanceof CelestialForgingAnvilPortalBlock
            && state.getValue(CelestialForgingAnvilPortalBlock.OPEN) != shouldBeOpen) {
            level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, shouldBeOpen), 3);
        }

        // Detect entities pressed against the portal slab and teleport them.
        // Leave detection: entities no longer touching are removed from the set,
        // so the next touch triggers another teleport.
        if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            AABB slabAABB = state.getShape(level, worldPosition).bounds().move(worldPosition);
            Direction facing = state.getValue(CelestialForgingAnvilPortalBlock.FACING);
            AABB detectionBox = slabAABB.expandTowards(
                facing.getStepX() * 0.05,
                facing.getStepY() * 0.05,
                facing.getStepZ() * 0.05
            );
            List<Entity> entities = level.getEntitiesOfClass(Entity.class, detectionBox);

            for (Entity entity : entities) {
                if (!touchingEntities.contains(entity.getUUID())) {
                    tryTouchTeleport(entity);
                    touchingEntities.add(entity.getUUID());
                }
            }

            Set<UUID> currentUuids = entities.stream()
                .map(Entity::getUUID)
                .collect(Collectors.toSet());
            touchingEntities.removeIf(uuid -> !currentUuids.contains(uuid));
        } else {
            touchingEntities.clear();
        }
    }

    /**
     * when an entity overlaps the 2px portal slab collision shape.
     * Checks {@link #touchingEntities} to ensure teleport fires only once
     * per touch — subsequent ticks while still inside the slab are skipped.
     * The BE tick clears the tracking when the entity leaves the slab.
     */
    public void tryTouchTeleport(Entity entity) {
        if (touchingEntities.contains(entity.getUUID())) return;

        CelestialForgingAnvilBlockEntity parent = findParentCfa();
        if (parent == null) return;
        Cube323PartHalf side = findSideFromParent(parent);
        if (side == null) return;

        WormholeNetwork network = WormholeNetwork.get();
        List<WormholeNetwork.Entry> connected = network.getConnected(
            parent.getWormholeParamsHash(), level.dimension(), parent.getBlockPos()
        );
        if (connected.size() != 1) return;

        WormholeNetwork.Entry target = connected.getFirst();
        if (!network.hasPortalAt(target.dimension(), target.pos(), side)) return;

        ServerLevel targetLevel = level.getServer().getLevel(target.dimension());
        if (targetLevel == null) return;

        BlockPos targetPortalPos = target.pos().offset(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
        Direction outwardFacing = CelestialForgingAnvilPortalBlock.getFacingFromSide(side);
        BlockPos destPos = targetPortalPos.relative(outwardFacing, 2);

        double dx = destPos.getX() + 0.5;
        double dy = destPos.getY();
        double dz = destPos.getZ() + 0.5;

        // Items and projectiles spawn 1 block higher so they don't land at feet level
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity
            || entity instanceof net.minecraft.world.entity.projectile.Projectile) {
            dy += 1;
        }

        // Only reverse the component perpendicular to the portal face;
        // horizontal and vertical movement parallel to the slab is preserved.
        net.minecraft.world.phys.Vec3 vel = entity.getDeltaMovement();
        net.minecraft.world.phys.Vec3 momentum;
        if (outwardFacing.getAxis() == Direction.Axis.Z) {
            momentum = new net.minecraft.world.phys.Vec3(vel.x, vel.y, -vel.z);
        } else {
            momentum = new net.minecraft.world.phys.Vec3(-vel.x, vel.y, vel.z);
        }
        float targetYRot = (entity.getYRot() + 180.0f) % 360.0f;
        entity.teleportTo(targetLevel, dx, dy, dz,
            Set.of(), targetYRot, entity.getXRot());
        entity.setDeltaMovement(momentum);

        touchingEntities.add(entity.getUUID());
    }

    @Nullable
    private Cube323PartHalf findSideFromParent(CelestialForgingAnvilBlockEntity parent) {
        for (var entry : parent.getPortals().entrySet()) {
            if (entry.getValue().equals(worldPosition)) {
                return entry.getKey();
            }
        }
        // Fallback: compute from position
        BlockPos cfaPos = parent.getBlockPos();
        int dx = worldPosition.getX() - cfaPos.getX();
        int dz = worldPosition.getZ() - cfaPos.getZ();
        if (dx == -1 && dz == 0) return Cube323PartHalf.BOTTOM_W;
        if (dx == 1 && dz == 0) return Cube323PartHalf.BOTTOM_E;
        if (dx == 0 && dz == -1) return Cube323PartHalf.BOTTOM_N;
        if (dx == 0 && dz == 1) return Cube323PartHalf.BOTTOM_S;
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
