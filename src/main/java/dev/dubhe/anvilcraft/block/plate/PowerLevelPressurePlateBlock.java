package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class PowerLevelPressurePlateBlock extends BasePressurePlateBlock {
    public static final MapCodec<PowerLevelPressurePlateBlock> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            BlockSetType.CODEC.fieldOf("block_set_type").forGetter(block -> block.type), propertiesCodec()
        ).apply(instance, PowerLevelPressurePlateBlock::new)
    );

    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public PowerLevelPressurePlateBlock(BlockSetType type, Properties properties) {
        super(properties, type);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected MapCodec<? extends BasePressurePlateBlock> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int i = this.getSignalForState(state);
        if (i > 0) {
            this.checkPressed(null, level, pos, state, i);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide) {
            int i = this.getSignalForState(state);
            if (i == 0) {
                this.checkPressed(entity, level, pos, state, i);
            }
        }
    }

    @Override
    protected int getSignalForState(BlockState state) {
        return state.getValue(POWER);
    }

    protected void checkPressed(@Nullable Entity entity, Level level, BlockPos pos, BlockState state, int currentSignal) {
        int expectedSignal = this.getSignalStrength(level, pos);
        boolean isActivating = currentSignal > 0;
        boolean needActivate = expectedSignal > 0;

        updateSignal(level, pos, state, currentSignal, expectedSignal);
        sendEvent(entity, level, pos, needActivate, isActivating);

        if (needActivate) {
            level.scheduleTick(new BlockPos(pos), this, this.getPressedTime());
        }
    }

    @Override
    protected int getSignalStrength(Level level, BlockPos pos) {
        return getSignalStrength(level, TOUCH_AABB.move(pos), getEntityClasses());
    }

    protected int getSignalStrength(Level level, AABB box, Set<Class<? extends Entity>> entityClasses) {
        int count = 0;

        for (Class<? extends Entity> entityClass : entityClasses) {
            count += level.getEntitiesOfClass(
                entityClass, box,
                EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())
            ).size();
        }

        return count > 0 ? 15 : 0;
    }

    protected Set<Class<? extends Entity>> getEntityClasses() {
        Class<? extends Entity> entityClass = switch (this.type.pressurePlateSensitivity()) {
            case EVERYTHING -> Entity.class;
            case MOBS -> LivingEntity.class;
        };
        HashSet<Class<? extends Entity>> newSet = Sets.newHashSet();
        newSet.add(entityClass);
        return newSet;
    }

    protected void updateSignal(Level level, BlockPos pos, BlockState state, int currentSignal, int expectedSignal) {
        if (currentSignal != expectedSignal) {
            BlockState blockstate = this.setSignalForState(state, expectedSignal);
            level.setBlock(pos, blockstate, 2);
            this.updateNeighbours(level, pos);
            level.setBlocksDirty(pos, state, blockstate);
        }
    }

    protected void sendEvent(@Nullable Entity entity, Level level, BlockPos pos, boolean isActivating, boolean needActivate) {
        if (!needActivate && isActivating) {
            level.playSound(null, pos, this.type.pressurePlateClickOff(), SoundSource.BLOCKS);
            level.gameEvent(entity, GameEvent.BLOCK_DEACTIVATE, pos);
        } else if (needActivate && !isActivating) {
            level.playSound(null, pos, this.type.pressurePlateClickOn(), SoundSource.BLOCKS);
            level.gameEvent(entity, GameEvent.BLOCK_ACTIVATE, pos);
        }
    }

    @Override
    protected BlockState setSignalForState(BlockState state, int strength) {
        return state.setValue(POWER, Math.clamp(strength, 0, 15));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }
}
