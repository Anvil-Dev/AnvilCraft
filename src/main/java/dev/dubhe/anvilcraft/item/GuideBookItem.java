package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.thought.Thinkable;
import dev.dubhe.anvilcraft.integration.IntegrationUtil;
import dev.dubhe.anvilcraft.network.OpenIntegrationScreenPacket;
import dev.dubhe.anvilcraft.util.ModEventUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class GuideBookItem extends Item implements Thinkable {
    public GuideBookItem(Properties properties) {
        super(properties.attributes(ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 3.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .build()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (ModEventUtil.hasGuideBook()) {
                ModEventUtil.openGuideBook(level, serverPlayer, usedHand);
                return new InteractionResultHolder<>(InteractionResult.CONSUME, player.getItemInHand(usedHand));
            } else {
                serverPlayer.connection.send(new OpenIntegrationScreenPacket());
            }
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(usedHand));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        this.appendHoverText(tooltipComponents);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onThought() {
        IntegrationUtil.openIntegrationScreen();
    }
}
