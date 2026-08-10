package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.inventory.FrostSmithingMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class FrostSmithingPackets {
    private static <T extends IPacket> Type<T> of(String path) {
        return IPacket.type(AnvilCraft.of("frost_smithing_" + path));
    }

    public record OriginalSync(int selected, List<RecipeResult> results) implements IClientboundPacket {
        public static final Type<OriginalSync> TYPE = FrostSmithingPackets.of("sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, OriginalSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            OriginalSync::selected,
            RecipeResult.STREAM_CODEC.apply(ByteBufCodecs.list()),
            OriginalSync::results,
            OriginalSync::new
        );

        @Override
        public Type<OriginalSync> type() {
            return OriginalSync.TYPE;
        }

        @Override
        public void handleOnClient(Player player) {
            if (!(player.containerMenu instanceof FrostSmithingMenu menu)) return;
            menu.sync(this.selected, this.results);
        }
    }

    public record ClickButton(boolean left) implements IServerboundPacket {
        public static final Type<ClickButton> TYPE = FrostSmithingPackets.of("click_button");
        public static final StreamCodec<ByteBuf, ClickButton> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClickButton::left,
            ClickButton::new
        );

        @Override
        public Type<ClickButton> type() {
            return ClickButton.TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            if (!(player.containerMenu instanceof FrostSmithingMenu menu)) return;
            menu.turn(this.left);
        }
    }
}
