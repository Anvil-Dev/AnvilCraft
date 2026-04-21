package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.IEmberBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.EmberSmithingMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Objects;

@Getter
@Setter
public class EmberSmithingTableBlock extends SmithingTableBlock implements IHammerRemovable, IEmberBlock {
    private static final Component CONTAINER_TITLE = Component.translatable("container.upgrade");

    private BlockState checkBlockState;

    public EmberSmithingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        return Util.interactionResultConverter().apply(this.use(state, level, pos, player, hand, hitResult));
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        return this.use(state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
    }

    @SuppressWarnings("UnreachableCode")
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ModMenuTypes.open((ServerPlayer) player, Objects.requireNonNull(state.getMenuProvider(level, pos)));
        player.awardStat(Stats.INTERACT_WITH_SMITHING_TABLE);
        return InteractionResult.CONSUME;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
            (i, inventory, player) -> new EmberSmithingMenu(i, inventory, ContainerLevelAccess.create(level, pos)),
            CONTAINER_TITLE);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        if (random.nextDouble() <= 0.5) {
            tryAbsorbWater(level, pos);
        }
    }
}
