package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.integration.patchouli.PatchouliUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                Class.forName("vazkii.patchouli.api.PatchouliAPI");
                PatchouliUtil.openBook(serverPlayer);
                return new InteractionResultHolder<>(InteractionResult.CONSUME, player.getItemInHand(usedHand));
            } catch (ClassNotFoundException ignore) {
                serverPlayer.sendSystemMessage(
                    Component.translatable("message.anvilcraft.need_patchouli_installed")
                        .withStyle(ChatFormatting.RED)
                );
            }
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(usedHand));
    }
}
