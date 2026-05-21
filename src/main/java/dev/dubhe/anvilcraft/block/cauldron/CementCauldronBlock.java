package dev.dubhe.anvilcraft.block.cauldron;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class CementCauldronBlock extends BaseCauldronBlock implements IHammerRemovable {
    public static final MapCodec<CementCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        propertiesCodec(), Color.CODEC.fieldOf("color").forGetter(CementCauldronBlock::getColor)
    ).apply(ins, CementCauldronBlock::new));

    private final Color color;

    public CementCauldronBlock(Properties properties, Color color) {
        super(properties, ModInteractionMap.CEMENT);
        this.color = color;
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return 0.9375;
    }

    @Override
    public boolean isFull(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return 3;
    }
}
