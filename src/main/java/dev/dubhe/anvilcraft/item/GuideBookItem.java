package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.thought.Thinkable;
import dev.dubhe.anvilcraft.api.thought.ThoughtManager;
import dev.dubhe.anvilcraft.integration.IntegrationUtil;
import dev.dubhe.anvilcraft.network.OpenIntegrationScreenPacket;
import dev.dubhe.anvilcraft.util.ModEventUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforgespi.Environment;

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
        if (!Environment.get().getDist().isClient()) {
            return;
        }
        long lastThoughtTime = ThoughtManager.getLastThoughtTime();
        if (lastThoughtTime <= 0) {
            tooltipComponents.add(
                Component.translatable(
                    "tooltip.anvilcraft.thought",
                    Component.keybind("key.anvilcraft.thought")
                ).withStyle(ChatFormatting.GRAY)
            );
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long curTime = minecraft.gui.getGuiTicks();
        long deltaTime = curTime - lastThoughtTime;
        final int maxPlaceholderCount = 20;
        final double maxSeconds = ThoughtManager.getMAX_SECONDS();
        int placeholderCount = (int) Math.floor(Math.min(deltaTime, 20 * maxSeconds) / (20 * maxSeconds) * maxPlaceholderCount);
        int blankCount = maxPlaceholderCount - placeholderCount;
        tooltipComponents.add(Component.translatable("tooltip.anvilcraft.thought.progress",
            "||".repeat(Math.max(0, placeholderCount)), " ".repeat(Math.max(0, blankCount))).withStyle(ChatFormatting.GRAY));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onThought() {
        IntegrationUtil.openIntegrationScreen();
    }
}
