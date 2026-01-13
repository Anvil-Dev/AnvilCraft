package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.DeflectionRingBlock;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.network.UpdateDeflectionRingLastEntitySpeedPacket;
import dev.dubhe.anvilcraft.util.AccelerateManager;
import dev.dubhe.anvilcraft.util.DistanceComparator;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class DeflectionRingBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final HashMap<Level, HashSet<BlockPos>> LEVEL_DEFLECTION_BLOCK_MAP = new HashMap<>();
    @Getter
    @Setter
    private PowerGrid grid;

    @Setter
    @Getter
    private double lastEntitySpeed = 0;
    private int resetEntitySpeedTickCounter = 0;

    @Getter
    private boolean overSpeed = false;
    private int overSpeedTick = 0;

    public DeflectionRingBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DEFLECTION_RING.get(), pos, blockState);
    }

    private DeflectionRingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static DeflectionRingBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new DeflectionRingBlockEntity(type, pos, blockState);
    }

    public static Iterable<BlockPos> getAllBlocks(Level level) {
        if (LEVEL_DEFLECTION_BLOCK_MAP.containsKey(level)) {
            return LEVEL_DEFLECTION_BLOCK_MAP.get(level);
        } else {
            return List.of();
        }
    }

    public static void clear() {
        LEVEL_DEFLECTION_BLOCK_MAP.clear();
    }

    private void addSelfToMap() {
        if (level == null) return;
        if (LEVEL_DEFLECTION_BLOCK_MAP.containsKey(level)) {
            LEVEL_DEFLECTION_BLOCK_MAP.get(level).add(getBlockPos());
        } else {
            HashSet<BlockPos> set = new HashSet<>();
            set.add(getBlockPos());
            LEVEL_DEFLECTION_BLOCK_MAP.put(level, set);
        }
    }

    private void removeSelfFromMap() {
        if (level == null) return;
        if (LEVEL_DEFLECTION_BLOCK_MAP.containsKey(level)) {
            LEVEL_DEFLECTION_BLOCK_MAP.get(level).remove(getBlockPos());
        }
    }

    private void updateLastEntitySpeed(Double speed) {
        this.resetEntitySpeedTickCounter = 0;
        this.lastEntitySpeed = speed;
        BlockState state = getBlockState();
        if (level == null) return;
        if (!(state.getBlock() instanceof DeflectionRingBlock block)) return;
        block.forEachPart(level, getBlockPos(), it -> level.updateNeighbourForOutputSignal(it, level.getBlockState(it).getBlock()));
        if (!(level instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            new ChunkPos(getBlockPos()),
            new UpdateDeflectionRingLastEntitySpeedPacket(getBlockPos(), lastEntitySpeed)
        );
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putDouble("lastEntitySpeed", lastEntitySpeed);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("lastEntitySpeed")) {
            this.lastEntitySpeed = tag.getDouble("entity");
        }
        super.loadAdditional(tag, provider);
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return level;
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public PowerComponentType getComponentType() {
        if (level == null) return PowerComponentType.INVALID;
        if (!level.getBlockState(getBlockPos()).hasProperty(DeflectionRingBlock.HALF)) return PowerComponentType.INVALID;
        if (level.getBlockState(getBlockPos()).getValue(DeflectionRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) {
            return PowerComponentType.CONSUMER;
        }
        return PowerComponentType.INVALID;
    }

    @Override
    public int getRange() {
        return 1;
    }

    public boolean isWork() {
        BlockState state = getBlockState();
        return state.getValue(DeflectionRingBlock.SWITCH) == Switch.ON && !state.getValue(DeflectionRingBlock.OVERLOAD);
    }

    public void tick() {
        if (level == null) return;
        if (resetEntitySpeedTickCounter >= 40 && !level.isClientSide) updateLastEntitySpeed(0.0);
        else resetEntitySpeedTickCounter++;
        if (overSpeed && overSpeedTick > 1) {
            overSpeed = false;
            overSpeedTick = 0;
            BlockState state = getBlockState();
            if (!(state.getBlock() instanceof DeflectionRingBlock block)) return;
            block.updateState(level, getBlockPos(), DeflectionRingBlock.OVERLOAD, state.getValue(DeflectionRingBlock.OVERLOAD), 3);
        } else if (overSpeed) {
            overSpeedTick++;
        }
        if (level.isClientSide) {
            if (!getBlockState().getValue(DeflectionRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
            if (isWork()) {
                addSelfToMap();
                accelerate();
            } else removeSelfFromMap();
        }
        if (grid == null) return;
        BlockState state = getBlockState();
        if (!state.getValue(DeflectionRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
        if (!(state.getBlock() instanceof DeflectionRingBlock block)) return;
        if (grid.isWorking() && state.getValue(DeflectionRingBlock.OVERLOAD)) {
            block.updateState(level, getBlockPos(), DeflectionRingBlock.OVERLOAD, false, 3);
        } else if (!grid.isWorking() && !state.getValue(DeflectionRingBlock.OVERLOAD)) {
            block.updateState(level, getBlockPos(), DeflectionRingBlock.OVERLOAD, true, 3);
        }
        if (!isWork()) {
            removeSelfFromMap();
            return;
        }
        addSelfToMap();
        if (state.getValue(DeflectionRingBlock.FACING).getAxis().equals(Direction.Axis.Y)) attractGianAnvil();
        accelerate();
    }

    private double fixPos(double p1, double p2, double p3) {
        double d = p1 * 0.99 / (Math.sqrt(p2 * p2 + p3 * p3));
        return Double.isNaN(d) ? 0 : d;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void accelerate() {
        if (this.level == null) return;
        List<Entity> entities2 = this.level.getEntitiesOfClass(
            Entity.class,
            new AABB(getBlockPos()),
            AccelerateManager::canBeAccelerated
        );
        for (Entity entity : entities2) {
            if (entity.getDeltaMovement().length() > Integer.MAX_VALUE * 0.99f) {
                this.overSpeed = true;
                BlockState state = getBlockState();
                if (!(state.getBlock() instanceof DeflectionRingBlock block)) return;
                block.updateState(this.level, getBlockPos(), DeflectionRingBlock.OVERLOAD, state.getValue(DeflectionRingBlock.OVERLOAD), 3);
            }
            Vec3 v = entity.getDeltaMovement();
            Direction facing = getBlockState().getValue(DeflectionRingBlock.FACING);
            boolean applyOffset = entity instanceof FallingBlockEntity || entity instanceof Player;
            final Vec3 fixedPos = switch (facing) {
                case UP -> new Vec3(
                    fixPos(v.z, v.z, v.x),
                    applyOffset ? -0.5 : 0,
                    -fixPos(v.x, v.z, v.x)
                );
                case DOWN -> new Vec3(
                    -fixPos(v.z, v.z, v.x),
                    applyOffset ? -0.5 : 0,
                    fixPos(v.x, v.z, v.x)
                );
                case NORTH -> new Vec3(
                    fixPos(v.y, v.y, v.x),
                    -fixPos(v.x, v.y, v.x) + (applyOffset && Math.abs(v.y) > Math.abs(v.x) ? -0.5 : 0),
                    0
                );
                case SOUTH -> new Vec3(
                    -fixPos(v.y, v.y, v.x),
                    fixPos(v.x, v.y, v.x) + (applyOffset && Math.abs(v.y) > Math.abs(v.x) ? -0.5 : 0),
                    0
                );
                case WEST -> new Vec3(
                    0,
                    fixPos(v.z, v.z, v.y) + (applyOffset && Math.abs(v.y) > Math.abs(v.z) ? -0.5 : 0),
                    -fixPos(v.y, v.z, v.y)
                );
                case EAST -> new Vec3(
                    0,
                    -fixPos(v.z, v.z, v.y) + (applyOffset && Math.abs(v.y) > Math.abs(v.z) ? -0.5 : 0),
                    fixPos(v.y, v.z, v.y)
                );
            };
            v = switch (facing) {
                case UP -> new Vec3(v.z, 0, -v.x);
                case DOWN -> new Vec3(-v.z, 0, v.x);
                case NORTH -> new Vec3(v.y, -v.x, 0);
                case SOUTH -> new Vec3(-v.y, v.x, 0);
                case WEST -> new Vec3(0, v.z, -v.y);
                case EAST -> new Vec3(0, -v.z, v.y);
            };
            entity.setDeltaMovement(v);
            if (entity instanceof Player) {
                double d0 = v.x;
                double d1 = v.y;
                double d2 = v.z;
                double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                entity.setXRot(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 180.0F / (float) Math.PI))));
                entity.setYRot(Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 180.0F / (float) Math.PI) - 90.0F));
                entity.setYHeadRot(entity.getYRot());
            }
            Vec3 blockCenter = getBlockPos().getCenter();
            entity.setPos(fixedPos.add(blockCenter));
        }
        List<Entity> entities = level.getEntitiesOfClass(
            Entity.class,
            AABB.encapsulatingFullBlocks(getBlockPos().east().north(), getBlockPos().west().south()),
            AccelerateManager::canBeAccelerated
        );
        for (Entity entity : entities) {
            if (
                entity.position().y
                - getBlockPos().getCenter().y
                - (entity instanceof FallingBlockEntity || entity instanceof Player ? 0.5 : 0)
                >= entity.getGravity()
            ) {
                return;
            }
            entity.setDeltaMovement(entity.getDeltaMovement().scale(1.0204081632653061));
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, entity.getGravity(), 0));
            if (level.isClientSide) continue;
            updateLastEntitySpeed(entity.getDeltaMovement().length());
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public void attractGianAnvil() {
        assert level != null;
        if (
            level.getBlockState(getBlockPos().below(2)).hasProperty(GiantAnvilBlock.HALF)
            && level.getBlockState(getBlockPos().below(2)).getValue(GiantAnvilBlock.HALF) == Cube3x3PartHalf.TOP_CENTER
        ) {
            return;
        }
        BlockPos giantAnvilPos = null;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        checkPos.set(getBlockPos().below(2));
        for (int y = 0; y < 11; y++) {
            BlockState checkState = level.getBlockState(checkPos);
            if (!checkState.hasProperty(GiantAnvilBlock.HALF)) {
                checkPos.move(Direction.DOWN);
                continue;
            }
            Cube3x3PartHalf cube3x3PartHalf = checkState.getValue(GiantAnvilBlock.HALF);
            if (cube3x3PartHalf == Cube3x3PartHalf.MID_CENTER) {
                giantAnvilPos = checkPos.west(0);
                break;
            }
            checkPos.move(Direction.DOWN);
        }
        Vector2d vector2d = new Vector2d(getBlockPos().getCenter().x, getBlockPos().getCenter().z);
        Optional<FallingGiantAnvilEntity> fallingGiantAnvilEntity = level.getEntitiesOfClass(FallingGiantAnvilEntity.class, new AABB(
                        getBlockPos().getX(),
                        getBlockPos().getY() - 2,
                        getBlockPos().getZ(),
                        getBlockPos().getX() + 1,
                        getBlockPos().getY() - 12,
                        getBlockPos().getZ() + 1
                )).stream()
                .sorted((e1, e2) -> new DistanceComparator(getBlockPos().getCenter()).compare(e1.position(), e2.position()))
                .filter(entity -> vector2d.distance(entity.position().x, entity.position().z) <= 0.25)
                .findFirst();
        if (fallingGiantAnvilEntity.isPresent()) {
            if (
                giantAnvilPos != null
                && fallingGiantAnvilEntity.get().position().distanceTo(getBlockPos().getCenter())
                   < giantAnvilPos.getCenter().distanceTo(getBlockPos().getCenter())
            ) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.get().position());
            } else if (giantAnvilPos == null) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.get().position());
            }
        }
        if (giantAnvilPos == null) return;
        checkPos.set(giantAnvilPos);
        checkPos.move(-1, 2, -1);
        while (checkPos.getY() < getBlockPos().getY() - 1) {
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    BlockState checked = level.getBlockState(checkPos);
                    if (!checked.canBeReplaced()) return;
                    checkPos.move(0, 0, 1);
                }
                checkPos.move(0, 0, -3);
                checkPos.move(1, 0, 0);
            }
            checkPos.move(-3, 1, 0);
        }
        Block block = level.getBlockState(giantAnvilPos.below()).getBlock();
        if (block instanceof GiantAnvilBlock giantAnvilBlock) {
            giantAnvilBlock.removePartsAndUpdate(level, giantAnvilPos.below());
        }
        BlockPos newPos = getBlockPos().below(4);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            level.setBlockAndUpdate(newPos.offset(part.getOffset()), ModBlocks.GIANT_ANVIL.getDefaultState()
                    .setValue(GiantAnvilBlock.HALF, part)
                    .setValue(GiantAnvilBlock.CUBE, part.equals(Cube3x3PartHalf.MID_CENTER) ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER)
            );
        }
        fallingGiantAnvilEntity.ifPresent(Entity::kill);
    }

    @Override
    public int getInputPower() {
        return getBlockState().getValue(DeflectionRingBlock.SWITCH) == Switch.ON ? 256 : 0;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeSelfFromMap();
    }
}
