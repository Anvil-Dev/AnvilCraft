package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.DragonRodItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record SwitchDragonRodProtectContainersPacket(InteractionHand hand, boolean protect) implements IServerboundPacket {
    public static final Type<SwitchDragonRodProtectContainersPacket> TYPE = new Type<>(
        AnvilCraft.of("switch_dragon_rod_protect_containers")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchDragonRodProtectContainersPacket> STREAM_CODEC =
        StreamCodec.composite(
            StreamCodecUtil.enumStreamCodec(InteractionHand.class),
            SwitchDragonRodProtectContainersPacket::hand,
            ByteBufCodecs.BOOL,
            SwitchDragonRodProtectContainersPacket::protect,
            SwitchDragonRodProtectContainersPacket::new
        );

    @Override
    public Type<SwitchDragonRodProtectContainersPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        ItemStack dragonRod = player.getItemInHand(this.hand);
        if (!(dragonRod.getItem() instanceof DragonRodItem)) return;
        dragonRod.set(ModComponents.DEVOUR_PROTECT_CONTAINERS, this.protect);
    }
}
