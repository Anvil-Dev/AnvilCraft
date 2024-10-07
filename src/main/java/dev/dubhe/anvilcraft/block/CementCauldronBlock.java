package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterAbstractCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;

import dev.dubhe.anvilcraft.util.ModInteractionMap;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.phys.BlockHitResult;
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
        super(properties, ModInteractionMap.CEMENT);
        this.color = color;
    }


    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        CauldronInteraction interaction = this.interactions.map().get(stack.getItem());
        if (interaction == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return interaction.interact(state, level, pos, player, hand, stack);
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
