package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HeavyIronWallBlock extends WallBlock implements IHammerRemovable {
    public HeavyIronWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean supportsExternalFaceHiding(BlockState state) {
        return true;
    }
}
