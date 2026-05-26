package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.HammerChangeBlockEvent;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.StateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HammerChangeBlockPacket(
    BlockPos pos,
    BlockState state
) implements CustomPacketPayload {
    public static final Type<HammerChangeBlockPacket> TYPE = new Type<>(AnvilCraft.of("hammer_change_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HammerChangeBlockPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            HammerChangeBlockPacket::pos,
            CodecUtil.BLOCK_STATE_STREAM_CODEC,
            HammerChangeBlockPacket::state,
            HammerChangeBlockPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!level.isLoaded(this.pos)) return;
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
        });
    }
}
