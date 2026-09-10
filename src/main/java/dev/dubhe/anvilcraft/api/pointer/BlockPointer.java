package dev.dubhe.anvilcraft.api.pointer;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil.MultiblockPart;
import dev.dubhe.anvilcraft.util.BlockStateAndEntity;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;

@Getter
public class BlockPointer implements ITargetPointer {
    /**
     * 搬运方块时使用的更新标志位：<br>
     * {@code UPDATE_ALL} 补上缺失的邻居更新，并让方块自身完成形状与存活自检；<br>
     * {@code UPDATE_MOVE_BY_PISTON} 把本次写入标记为活塞式移动，
     * 使 {@code onPlace}/{@code onRemove} 收到 {@code movedByPiston = true}
     * （如鱼缸、加工台据此在搬运时保留内容物）。
     */
    private static final int MOVE_UPDATE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_MOVE_BY_PISTON;

    private final Type type;
    private final BlockPos pos;
    private final BlockState state;
    private @Nullable BlockEntity be;

    public BlockPointer(Type type, BlockPos pos, BlockState state) {
        this.type = type;
        this.pos = pos;
        this.state = state;
    }

    public BlockPointer(Type type, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        this(type, pos, state);
        this.be = be;
    }

    @Override
    public boolean isStillValid(Level level) {
        if (this.state.isAir()) {
            return false;
        }
        BlockState current = level.getBlockState(this.pos);
        if (!current.is(this.state.getBlock())) {
            return false;
        }
        for (Property<?> property : this.state.getValues().keySet()) {
            if (current.getOptionalValue(property).filter(value -> value.equals(this.state.getValue(property))).isEmpty()) {
                return false;
            }
        }
        this.be = level.getBlockEntity(this.pos);
        return true;
    }

    @Override
    public boolean matches(Level level, BlockState requiredState) {
        return this.state.is(requiredState.getBlock());
    }

    @Override
    public Either<ItemStack, BlockStateAndEntity> getDisplayedBlock() {
        return Either.right(new BlockStateAndEntity(this.state, this.be));
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos) {
        BlockState targetState = this.state;
        if (
            targetState.hasProperty(BlockStateProperties.WATERLOGGED)
            && targetState.getValue(BlockStateProperties.WATERLOGGED)
        ) {
            targetState = targetState.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
        }
        return this.moveToPos(level, pos, targetState, false);
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos, BlockState requiredState) {
        if (!this.matches(level, requiredState)) {
            return false;
        }
        BlockState targetState = BlockPlacementRules.applyBlueprintStateRules(
            level.registryAccess(),
            this.state,
            requiredState
        );
        return this.moveToPos(level, pos, targetState, true);
    }

    private boolean moveToPos(ServerLevel level, BlockPos pos, BlockState targetState, boolean applyBlueprintRules) {
        if (!this.isStillValid(level)) {
            return false;
        }
        List<MultiblockPart> sourceParts = BlockPlacementUtil.getPresentMultiblockParts(
            level,
            this.pos,
            this.state
        );
        List<MultiblockPart> targetParts = BlockPlacementUtil.getExpectedMultiblockParts(pos, targetState);
        if (sourceParts.isEmpty() || sourceParts.size() != targetParts.size()) {
            return false;
        }

        List<MovingPart> movingParts = new ArrayList<>(sourceParts.size());
        for (int index = 0; index < sourceParts.size(); index++) {
            MultiblockPart sourcePart = sourceParts.get(index);
            MultiblockPart targetPart = targetParts.get(index);
            if (!BlockPlacementUtil.isTargetAvailable(level, targetPart.pos())) {
                return false;
            }
            BlockState partTargetState = applyBlueprintRules
                ? BlockPlacementRules.applyBlueprintStateRules(
                    level.registryAccess(),
                    sourcePart.state(),
                    targetPart.state()
                )
                : clearWaterlogged(sourcePart.state());
            // 副部件（门/植物的上半、床尾等）的存活取决于同批搬运的主部件，此处跳过存活校验
            if (!BlockPlacementUtil.isSecondaryMultiblockPart(partTargetState)
                && !partTargetState.canSurvive(level, targetPart.pos())) {
                return false;
            }
            movingParts.add(new MovingPart(
                sourcePart,
                targetPart.pos(),
                partTargetState,
                level.getBlockState(targetPart.pos()),
                level.getBlockEntity(sourcePart.pos())
            ));
        }

        // 记录移动前目标位置附近的实体，用于识别被放置后自身转化为实体的方块（如被红石激活的 TNT）
        Set<UUID> entitiesBeforeMove = new HashSet<>();
        for (MovingPart part : movingParts) {
            for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(part.targetPos()).inflate(0.5))) {
                entitiesBeforeMove.add(entity.getUUID());
            }
        }

        for (MovingPart part : movingParts) {
            if (part.entity() != null) {
                level.removeBlockEntity(part.source().pos());
            }
            if (!level.setBlock(
                part.source().pos(),
                part.source().state().getFluidState().createLegacyBlock(),
                MOVE_UPDATE_FLAGS
            )) {
                restoreParts(level, movingParts);
                return false;
            }
        }
        for (MovingPart part : movingParts) {
            // 与取物/蓝图模式一致：目标格使用包含邻居更新的标志位，
            // 保证红石、比较器以及方块自身的形状/存活校验都能正常触发
            if (!level.setBlock(part.targetPos(), part.targetState(), MOVE_UPDATE_FLAGS)) {
                restoreParts(level, movingParts);
                return false;
            }
        }
        for (MovingPart part : movingParts) {
            BlockEntity entity = part.entity();
            if (entity != null) {
                entity.worldPosition = part.targetPos();
                entity.clearRemoved();
                level.removeBlockEntity(part.targetPos());
                level.setBlockEntity(entity);
                if (part.targetState().getBlock() instanceof IMoveableEntityBlock block) {
                    block.notifyMoved(level, part.targetPos(), part.targetState(), entity);
                }
            }
        }
        for (MovingPart part : movingParts) {
            level.updateNeighborsAt(part.source().pos(), part.source().state().getBlock());
            level.updateNeighborsAt(part.targetPos(), part.targetState().getBlock());
            // 与取物模式的 setBlock(UPDATE_ALL) 一致：有模拟信号输出的方块需要通知比较器更新
            if (part.targetState().hasAnalogOutputSignal()) {
                level.updateNeighbourForOutputSignal(part.targetPos(), part.targetState().getBlock());
            }
        }
        if (BlockPlacementUtil.getPresentMultiblockParts(level, pos, targetState).size() != targetParts.size()) {
            // 目标方块被自身 onPlace 转化为实体（如 TNT 被红石激活引爆）时源方块已被消耗，
            // 与取物模式一致视为放置成功，不再回滚源方块，避免刷方块
            boolean transformedIntoEntity = movingParts.stream().anyMatch(part -> level
                .getEntitiesOfClass(Entity.class, new AABB(part.targetPos()).inflate(0.5))
                .stream()
                .anyMatch(entity -> !entitiesBeforeMove.contains(entity.getUUID())));
            if (!transformedIntoEntity) {
                restoreParts(level, movingParts);
                return false;
            }
        }
        // 触发玩家放置相关的代码，与取物模式一致（放置回调、放置音效、放置进度）
        for (MovingPart part : movingParts) {
            BlockState placedState = level.getBlockState(part.targetPos());
            if (!placedState.is(part.targetState().getBlock())) {
                continue;
            }
            if (movingParts.size() == 1) {
                placedState.getBlock().setPlacedBy(level, part.targetPos(), placedState, null, ItemStack.EMPTY);
            }
            SoundType soundType = placedState.getSoundType(level, part.targetPos(), null);
            level.playSound(
                null,
                part.targetPos(),
                soundType.getPlaceSound(),
                SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F
            );
            TriggerUtil.placerPlaceBlock(level, part.targetPos(), placedState.getBlock());
        }
        return true;
    }

    private static BlockState clearWaterlogged(BlockState state) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
            && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
        }
        return state;
    }

    private static void restoreParts(ServerLevel level, List<MovingPart> movingParts) {
        for (MovingPart part : movingParts) {
            level.removeBlockEntity(part.targetPos());
            level.setBlock(part.targetPos(), part.previousTargetState(), MOVE_UPDATE_FLAGS);
        }
        for (MovingPart part : movingParts) {
            level.setBlock(part.source().pos(), part.source().state(), MOVE_UPDATE_FLAGS);
            BlockEntity entity = part.entity();
            if (entity != null) {
                entity.worldPosition = part.source().pos();
                entity.clearRemoved();
                level.removeBlockEntity(part.source().pos());
                level.setBlockEntity(entity);
            }
        }
        for (MovingPart part : movingParts) {
            level.updateNeighborsAt(part.targetPos(), part.previousTargetState().getBlock());
            level.updateNeighborsAt(part.source().pos(), part.source().state().getBlock());
        }
    }

    private record MovingPart(
        MultiblockPart source,
        BlockPos targetPos,
        BlockState targetState,
        BlockState previousTargetState,
        @Nullable BlockEntity entity
    ) {
    }

    public static class Type implements ITargetPointer.Type<BlockPointer> {
        public static final Codec<Type> CODEC = ModRegistries.TARGET_POINTER_TYPE.byNameCodec().flatXmap(
            raw -> raw instanceof Type type
                   ? DataResult.success(type)
                   : DataResult.error(() -> "Cannot cast %s to BlockPointer.Type".formatted(raw.getClass().getSimpleName())),
            DataResult::success
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC = ByteBufCodecs.registry(
            ModRegistryKeys.TARGET_POINTER_TYPE
        ).map(Util::cast, Function.identity());
        public static final MapCodec<BlockPointer> POINTER_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Type.CODEC
                .fieldOf("type")
                .forGetter(BlockPointer::getType),
            BlockPos.CODEC
                .fieldOf("pos")
                .forGetter(BlockPointer::getPos),
            BlockState.CODEC
                .fieldOf("state")
                .forGetter(BlockPointer::getState)
        ).apply(inst, BlockPointer::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockPointer> POINTER_STREAM_CODEC = StreamCodec.composite(
            Type.STREAM_CODEC,
            BlockPointer::getType,
            BlockPos.STREAM_CODEC,
            BlockPointer::getPos,
            StreamCodecUtil.BLOCK_STATE,
            BlockPointer::getState,
            BlockPointer::new
        );

        @Override
        public MapCodec<BlockPointer> codec() {
            return Type.POINTER_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlockPointer> streamCodec() {
            return Type.POINTER_STREAM_CODEC;
        }

        @Override
        public @Nullable BlockPointer point(
            Level level,
            BlockPos pos,
            Direction facing,
            @Nullable BlockState requiredState
        ) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return null;
            }
            if (requiredState != null && !state.is(requiredState.getBlock())) {
                return null;
            }
            if (requiredState != null
                && BlockPlacementUtil.isMultiblockBlock(requiredState)
                && BlockPlacementUtil.isSecondaryMultiblockPart(state)) {
                return null;
            }
            List<MultiblockPart> parts = BlockPlacementUtil.getPresentMultiblockParts(level, pos, state);
            if (parts.isEmpty()) {
                return null;
            }
            for (MultiblockPart part : parts) {
                if (!PistonBaseBlock.isPushable(
                    part.state(),
                    level,
                    part.pos(),
                    facing.getOpposite(),
                    false,
                    facing.getOpposite()
                )) {
                    return null;
                }
            }
            return new BlockPointer(this, pos, state, level.getBlockEntity(pos));
        }
    }
}
