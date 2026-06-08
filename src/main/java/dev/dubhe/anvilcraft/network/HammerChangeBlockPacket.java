package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.HammerChangeBlockEvent;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.StateUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record HammerChangeBlockPacket(BlockPos pos, BlockState state) implements IServerboundPacket {
    public static final Type<HammerChangeBlockPacket> TYPE = IPacket.type(AnvilCraft.of("hammer_change_block"));
    public static final StreamCodec<ByteBuf, HammerChangeBlockPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        HammerChangeBlockPacket::pos,
        StreamCodecUtil.BLOCK_STATE,
        HammerChangeBlockPacket::state,
        HammerChangeBlockPacket::new
    );

    @Override
    public Type<HammerChangeBlockPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        Level level = player.level();
        if (!level.isLoaded(this.pos)) {
            return;
        }
        BlockState blockState = level.getBlockState(this.pos);
        boolean stateVerified = StateUtil.verifyPossibleStatesForProperty(blockState, this.state);
        boolean hasHammer = player.getMainHandItem().getItem() instanceof AnvilHammerItem
                            || player.getOffhandItem().getItem() instanceof AnvilHammerItem;
        AttributeInstance attribute = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        double value = attribute == null ? 5.0 : attribute.getValue();
        boolean distanceVerified = this.pos.getCenter().distanceToSqr(player.getEyePosition()) <= value * value + 2;
        if (!HammerChangeBlockEvent.invoke(
            level,
            player,
            this.pos,
            this.state,
            blockState,
            hasHammer
            && stateVerified
            && distanceVerified
            && level.mayInteract(player, this.pos)
            && player.getAbilities().mayBuild
        )) {
            return;
        }
        level.setBlock(this.pos, this.state, Block.UPDATE_ALL_IMMEDIATE);
    }
}
