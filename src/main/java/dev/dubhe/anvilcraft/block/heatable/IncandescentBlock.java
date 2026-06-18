package dev.dubhe.anvilcraft.block.heatable;

import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class IncandescentBlock extends RedhotBlock {
    public IncandescentBlock(Properties properties) {
        super(properties, 4, 8);
    }

    @Override
    public HeatableBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.INCANDESCENT_BLOCK.create(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;

        Direction face = Direction.getRandom(random);
        double inset = 0.52;
        double x = pos.getX() + 0.5 + face.getStepX() * inset + (random.nextDouble() - 0.5) * 0.4;
        double y = pos.getY() + 0.5 + face.getStepY() * inset + (random.nextDouble() - 0.5) * 0.4;
        double z = pos.getZ() + 0.5 + face.getStepZ() * inset + (random.nextDouble() - 0.5) * 0.4;

        if (random.nextBoolean()) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.01 + random.nextDouble() * 0.02, 0.0);
        } else {
            level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
