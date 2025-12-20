package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.block.IMoveableEntityBlock;
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
import org.jetbrains.annotations.Nullable;

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
    public CompoundTag clearData(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SpaceOvercompressorBlockEntity s) {
            tag.putLong("storedMass", s.getStoredMass());
        }
        return tag;
    }

    @Override
    public void setData(Level level, BlockPos pos, CompoundTag nbt) {
        BlockEntity entity = level.getBlockEntity(pos);
        long mass = nbt.getLong("storedMass");
        if (entity instanceof SpaceOvercompressorBlockEntity s) {
            s.injectMass(mass - s.getStoredMass());
        }
    }
}
