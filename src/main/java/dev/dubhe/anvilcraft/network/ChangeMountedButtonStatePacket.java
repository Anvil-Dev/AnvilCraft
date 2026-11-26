package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.HammerChangeBlockEvent;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.property.component.ChromaticButtonMode;
import dev.dubhe.anvilcraft.util.StateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record ChangeMountedButtonStatePacket(
    BlockPos pos, Vec3 baseHit, BlockState initial, BlockState selected, Optional<ChromaticButtonMode> mode
)
    implements IServerboundPacket {
    public static final Type<ChangeMountedButtonStatePacket> TYPE = IPacket.type(AnvilCraft.of("change_mounted_button_state"));
    public static final StreamCodec<FriendlyByteBuf, ChangeMountedButtonStatePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ChangeMountedButtonStatePacket::pos,
        StreamCodecUtil.VEC3, ChangeMountedButtonStatePacket::baseHit,
        StreamCodecUtil.BLOCK_STATE, ChangeMountedButtonStatePacket::initial,
        StreamCodecUtil.BLOCK_STATE, ChangeMountedButtonStatePacket::selected,
        ByteBufCodecs.optional(ChromaticButtonMode.STREAM_CODEC), ChangeMountedButtonStatePacket::mode,
        ChangeMountedButtonStatePacket::new
    );

    public ChangeMountedButtonStatePacket(BlockPos pos, Vec3 baseHit, BlockState initial, BlockState selected) {
        this(pos, baseHit, initial, selected, Optional.empty());
    }

    @Override
    public Type<ChangeMountedButtonStatePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        var level = player.level();
        if (!player.isAlive() || player.isSpectator() || !level.hasChunkAt(this.pos) || !level.mayInteract(player, this.pos)
            || !(level.getBlockEntity(this.pos) instanceof BigRedButtonBlockEntity button)) {
            return;
        }
        var mounted = button.getMountedBlock();
        if (mounted == null || mounted.state() != this.initial || !mounted.canAdjustWithHammer(player)) {
            return;
        }
        if (this.mode.isPresent()) {
            if (!this.initial.is(ModBlocks.CHROMATIC_STONE) || this.selected != this.initial || this.mode.get() == mounted.mode()) {
                return;
            }
        } else if (this.initial.is(ModBlocks.CHROMATIC_STONE) || this.initial == this.selected
            || !StateUtil.verifyPossibleStatesForProperty(this.initial, this.selected)) {
            return;
        }
        var hit = BigRedButtonHoldPacket.visibleHit(player, button, this.baseHit, false);
        if (hit == null || !BigRedButtonBlock.isBaseHit(button.getBlockState(), level, hit)
            || !HammerChangeBlockEvent.invoke(level, player, this.pos, this.selected, this.initial, true)) {
            return;
        }
        if (this.mode.map(button::setChromaticMode).orElseGet(() -> button.setMountedState(this.selected))) {
            level.playSound(null, this.pos, ModSoundEvents.ANVIL_HAMMER_ROTATE_BLOCK.get(), SoundSource.BLOCKS, 2, 1);
        }
    }
}
