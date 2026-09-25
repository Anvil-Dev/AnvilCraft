package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.armor.EquipmentArmorItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record SwitchEquipmentAbilityPacket(InteractionHand hand, EquipmentSlot slot, boolean enabled) implements IServerboundPacket {
    public static final Type<SwitchEquipmentAbilityPacket> TYPE = new Type<>(AnvilCraft.of("switch_equipment_ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchEquipmentAbilityPacket> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.enumStreamCodec(InteractionHand.class), SwitchEquipmentAbilityPacket::hand,
        StreamCodecUtil.enumStreamCodec(EquipmentSlot.class), SwitchEquipmentAbilityPacket::slot,
        ByteBufCodecs.BOOL, SwitchEquipmentAbilityPacket::enabled,
        SwitchEquipmentAbilityPacket::new
    );

    @Override
    public Type<SwitchEquipmentAbilityPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        ItemStack stack = player.getItemInHand(this.hand);
        if (!(stack.getItem() instanceof EquipmentArmorItem armor) || armor.getEquipmentSlot() != this.slot) return;
        var component = EquipmentArmorItem.abilityComponent(stack);
        if (component != null) stack.set(component, this.enabled);
    }
}
