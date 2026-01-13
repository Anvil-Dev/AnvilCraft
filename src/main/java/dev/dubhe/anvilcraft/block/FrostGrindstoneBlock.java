package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.block.IFrostBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterGrindstoneBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.FrostGrindstoneMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@Getter
@Setter
public class FrostGrindstoneBlock extends BetterGrindstoneBlock implements IHammerRemovable, IFrostBlock {
    private static final Component CONTAINER_TITLE = Component.translatable("container.grindstone_title");

    public FrostGrindstoneBlock(Properties properties) {
        super(properties);
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
        ModMenuTypes.open((ServerPlayer) player, state.getMenuProvider(level, pos));
        player.awardStat(Stats.INTERACT_WITH_GRINDSTONE);
        return InteractionResult.CONSUME;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
            (i, inventory, player) -> new FrostGrindstoneMenu(i, inventory, ContainerLevelAccess.create(level, pos)),
            FrostGrindstoneBlock.CONTAINER_TITLE
        );
    }
}
