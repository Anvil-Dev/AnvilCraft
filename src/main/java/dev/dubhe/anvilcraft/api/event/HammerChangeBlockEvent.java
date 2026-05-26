package dev.dubhe.anvilcraft.api.event;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public class HammerChangeBlockEvent extends BlockEvent {
    @Getter
    private final Player player;
    @Getter
    private final BlockState oldState;
    @Getter
    @Setter
    private boolean isVerified;

    public HammerChangeBlockEvent(
        LevelAccessor level,
        Player player,
        BlockPos pos,
        BlockState state,
        BlockState oldState,
        boolean isVerified
    ) {
        super(level, pos, state);
        this.player = player;
        this.oldState = oldState;
        this.isVerified = isVerified;
    }

    public static boolean invoke(
        LevelAccessor level,
        Player player,
        BlockPos pos,
        BlockState state,
        BlockState oldState,
        boolean isVerified
    ) {
        HammerChangeBlockEvent event = new HammerChangeBlockEvent(level, player, pos, state, oldState, isVerified);
        NeoForge.EVENT_BUS.post(event);
        return event.isVerified();
    }
}
