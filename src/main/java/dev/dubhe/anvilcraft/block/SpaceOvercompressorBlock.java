package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
    public @NotNull CompoundTag clearData(@NotNull Level level, @NotNull BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SpaceOvercompressorBlockEntity s) {
            tag.putLong("storedMass", s.getStoredMass());
        }
        return tag;
    }

    @Override
    public void setData(@NotNull Level level, @NotNull BlockPos pos, @NotNull CompoundTag nbt) {
        BlockEntity entity = level.getBlockEntity(pos);
        long mass = nbt.getLong("storedMass");
        if (entity instanceof SpaceOvercompressorBlockEntity s) {
            s.injectMass(mass - s.getStoredMass());
        }
    }
}
