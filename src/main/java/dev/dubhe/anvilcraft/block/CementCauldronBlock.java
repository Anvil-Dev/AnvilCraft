package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterAbstractCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;

import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CementCauldronBlock extends BetterAbstractCauldronBlock implements IHammerRemovable {

    public static final MapCodec<CementCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        propertiesCodec(), Color.CODEC.fieldOf("color").forGetter(CementCauldronBlock::getColor)
    ).apply(ins, CementCauldronBlock::new));

    private final Color color;

    /**
     * @param properties 方块属性
     */
    public CementCauldronBlock(Properties properties, Color color) {
        super(properties, CauldronInteraction.EMPTY);
        this.color = color;
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected double getContentHeight(@NotNull BlockState state) {
        return 0.9375;
    }

    @Override
    public boolean isFull(@NotNull BlockState state) {
        return true;
    }
}
