package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.HammerChangeBlockEvent;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.StateUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

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
        // 嘴对嘴检测：新状态前方有溜槽朝向自己则爆炸，不设状态
        DirectionProperty facingProp = this.state.hasProperty(ChuteBlock.FACING)
            ? ChuteBlock.FACING : null;
        if (facingProp == null && this.state.hasProperty(BlockStateProperties.FACING)) {
            facingProp = BlockStateProperties.FACING;
        }
        if (facingProp != null) {
            Direction facing = this.state.getValue(facingProp);
            BlockState frontState = level.getBlockState(this.pos.relative(facing));
            if (ChuteBlock.isChuteBlock(frontState) && ChuteBlock.getFacing(frontState) == facing.getOpposite()) {
                BlockState oldState = level.getBlockState(this.pos);
                level.levelEvent(2001, this.pos, Block.getId(oldState));
                level.setBlock(this.pos, Blocks.AIR.defaultBlockState(), 3);
                Block.dropResources(oldState, level, this.pos);
                return;
            }
        }
        level.setBlock(this.pos, this.state, Block.UPDATE_ALL_IMMEDIATE);
    }
}
