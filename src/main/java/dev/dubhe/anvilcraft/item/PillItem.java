package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

import java.util.List;

public class PillItem extends Item {
    public PillItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack itemStack = super.getDefaultInstance();
        itemStack.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return itemStack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide()) {
            ItemStack itemStack = player.getItemInHand(usedHand);
            PotionContents potionContents = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            Boolean weakening = itemStack.getOrDefault(ModComponents.WEAKENING, false);
            potionContents.forEachEffect((effect) -> {
                if (weakening) {
                    effect = new MobEffectInstance(
                        effect.getEffect(),
                        effect.mapDuration((duration) -> duration / 4),
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.isVisible()
                    );
                }
                if (effect.getEffect().value().isInstantenous()) {
                    effect.getEffect().value().applyInstantenousEffect(player, player, player, effect.getAmplifier(), 1);
                } else {
                    player.addEffect(effect);
                }
            });
            itemStack.consume(1, player);
            player.getCooldowns().addCooldown(itemStack.getItem(), 40);
            return InteractionResultHolder.success(itemStack);
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return Potion.getName(
            stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion(),
            this.getDescriptionId() + ".effect."
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        PotionContents potionContents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Boolean weakening = stack.getOrDefault(ModComponents.WEAKENING, false);
        if (potionContents.potion().isEmpty()) {
            tooltipComponents.add(Component.translatable("item.anvilcraft.pill.tooltip"));
        } else {
            if (weakening) {
                potionContents.addPotionTooltip(tooltipComponents::add, 0.25F, context.tickRate());
            } else {
                potionContents.addPotionTooltip(tooltipComponents::add, 1.0F, context.tickRate());
            }
        }
    }
}
