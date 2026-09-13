package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RuinsStructure;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record RuinsUpdatePacket(BlockPos pos, String drops, boolean fragile) implements IServerboundPacket {
    public static final Type<RuinsUpdatePacket> TYPE = IPacket.type(AnvilCraft.of("ruins_update"));
    public static final StreamCodec<ByteBuf, RuinsUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, RuinsUpdatePacket::pos,
        ByteBufCodecs.stringUtf8(256), RuinsUpdatePacket::drops,
        ByteBufCodecs.BOOL, RuinsUpdatePacket::fragile,
        RuinsUpdatePacket::new
    );

    @Override
    public Type<RuinsUpdatePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !player.isCreative()
            || !player.level().isLoaded(this.pos)
            || !(player.getMainHandItem().is(ModBlocks.RUINS_BLOCK.asItem())
                || player.getOffhandItem().is(ModBlocks.RUINS_BLOCK.asItem()))) {
            return;
        }
        if (!(player.level().getBlockEntity(this.pos) instanceof RuinsBlockEntity ruins)) return;
        if (!RuinsStructure.canInteract(player, ruins)) return;
        // 内联表没有 ID，允许只切换交互模式而不覆盖现有战利品内容。
        if (!this.drops.equals(ruins.getDropsId())) {
            ResourceLocation id = ResourceLocation.tryParse(this.drops);
            if (id == null
                || !serverPlayer.server.reloadableRegistries().get().registryOrThrow(Registries.LOOT_TABLE).containsKey(id)) return;
            ruins.setDrops(id);
        }
        ruins.setFragile(this.fragile);
    }
}
