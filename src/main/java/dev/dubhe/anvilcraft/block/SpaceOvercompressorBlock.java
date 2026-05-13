package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SpaceOvercompressorBlock extends BetterBaseEntityBlock implements IHammerRemovable, IMoveableEntityBlock {
    public SpaceOvercompressorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SpaceOvercompressorBlock::new);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceOvercompressorBlockEntity(pos, state);
    }

    @Override
    public void loadData(Level level, BlockPos pos, ValueInput input) {
        BlockEntity entity = level.getBlockEntity(pos);
        long mass = input.getLongOr("storedMass", 0L);
        if (entity instanceof SpaceOvercompressorBlockEntity s) {
            s.injectMass(mass - s.getStoredMass());
        }
    }

    @Override
    public void storeData(Level level, BlockPos pos, ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SpaceOvercompressorBlockEntity s) {
            tag.putLong("storedMass", s.getStoredMass());
        }
    }
}
