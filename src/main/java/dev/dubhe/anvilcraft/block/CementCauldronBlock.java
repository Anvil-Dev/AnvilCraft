package dev.dubhe.anvilcraft.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterAbstractCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;

import dev.dubhe.anvilcraft.init.ModItems;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    public static final CauldronInteraction.InteractionMap CEMENT = new CauldronInteraction.InteractionMap(
        "cement",
        new ImmutableMap.Builder<Item, CauldronInteraction>()
            .put(
                Items.BUCKET,
                (state, level, pos, player, hand, stack) -> {
                    if (level.getBlockState(pos).getBlock() instanceof CementCauldronBlock cauldronBlock) {
                        Color color = cauldronBlock.color;
                        return CauldronInteraction.fillBucket(state, level, pos, player, hand, stack, ModItems.CEMENT_BUCKETS.get(color).asStack(), (s) -> true, SoundEvents.BUCKET_FILL);
                    }
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
            )
            .build()
    );

    public static final MapCodec<CementCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        propertiesCodec(), Color.CODEC.fieldOf("color").forGetter(CementCauldronBlock::getColor)
    ).apply(ins, CementCauldronBlock::new));

    private final Color color;

    /**
     * @param properties 方块属性
     */
    public CementCauldronBlock(Properties properties, Color color) {
        super(properties, CEMENT);
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
