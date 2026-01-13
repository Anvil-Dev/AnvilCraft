package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.recipe.util.CodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.inventory.FrostSmithingMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public class FrostSmithingPackets {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
            OriginalSync.TYPE,
            OriginalSync.STREAM_CODEC,
            OriginalSync.HANDLER
        );
        registrar.playToServer(
            ClickButton.TYPE,
            ClickButton.STREAM_CODEC,
            ClickButton.HANDLER
        );
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> of(String path) {
        return new CustomPacketPayload.Type<>(AnvilCraft.of("frost_smithing_" + path));
    }

    public record OriginalSync(int selected, List<RecipeResult> results) implements CustomPacketPayload {
        public static final Type<OriginalSync> TYPE = FrostSmithingPackets.of("sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, OriginalSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            OriginalSync::selected,
            RecipeResult.STREAM_CODEC.apply(ByteBufCodecs.list()),
            OriginalSync::results,
            OriginalSync::new
        );
        public static final IPayloadHandler<OriginalSync> HANDLER = OriginalSync::clientHandler;

        @Override
        public Type<OriginalSync> type() {
            return TYPE;
        }

        public void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player().containerMenu instanceof FrostSmithingMenu menu)) return;
                menu.sync(this.selected, this.results);
            });
        }
    }

    public record ClickButton(boolean left) implements CustomPacketPayload {
        public static final Type<ClickButton> TYPE = FrostSmithingPackets.of("click_button");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClickButton> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClickButton::left,
            ClickButton::new
        );
        public static final IPayloadHandler<ClickButton> HANDLER = ClickButton::serverHandler;

        @Override
        public Type<ClickButton> type() {
            return TYPE;
        }

        public void serverHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player().containerMenu instanceof FrostSmithingMenu menu)) return;
                menu.turn(this.left);
            });
        }
    }
}
