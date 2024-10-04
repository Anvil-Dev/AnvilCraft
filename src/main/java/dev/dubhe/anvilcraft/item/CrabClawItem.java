package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.EntityHelper;
import dev.dubhe.anvilcraft.api.entity.attribute.EntityReachAttribute;
import dev.dubhe.anvilcraft.init.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import com.google.common.collect.Multimap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CrabClawItem extends Item {

    public static final AttributeModifier RANGE_ATTRIBUTE_MODIFIER = new AttributeModifier(
        AnvilCraft.of("range_modifier"),
        3,
        AttributeModifier.Operation.ADD_VALUE
    );
    private static final Supplier<Multimap<Holder<Attribute>, AttributeModifier>> RANGE_MODIFIER_SUPPLIER =
        EntityReachAttribute.getRangeModifierSupplier(RANGE_ATTRIBUTE_MODIFIER);
    public static final String CRAB_CLAW_MARKER = "crabClaw";
    public static final String DUAL_CRAB_CLAW_MARKER = "dualCrabClaw";



    public CrabClawItem(Properties properties) {
        super(properties);
    }

    /**
     * 蟹钳增加交互距离
     */
    public static void holdingCrabClawIncreasesRange(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide) return;
        CompoundTag customData = EntityHelper.getCustomData(entity);
        boolean inOffHand = ModItems.CRAB_CLAW.isIn(player.getOffhandItem());
        boolean inMainHand = ModItems.CRAB_CLAW.isIn(player.getMainHandItem());
        boolean holdingDualCrabClaw = inOffHand && inMainHand;
        boolean holdingCrabClaw = inOffHand || inMainHand;
        boolean wasHoldingCrabClaw = customData.contains(CRAB_CLAW_MARKER);
        boolean wasHoldingDualCrabClaw = customData.contains(DUAL_CRAB_CLAW_MARKER);
        if (!holdingCrabClaw) {
            player.getAttributes().removeAttributeModifiers(RANGE_MODIFIER_SUPPLIER.get());
            if (wasHoldingCrabClaw) {
                customData.remove(CRAB_CLAW_MARKER);
            }
        } else {
            player.getAttributes().addTransientAttributeModifiers(RANGE_MODIFIER_SUPPLIER.get());
            if (!wasHoldingDualCrabClaw) {
                customData.putBoolean(CRAB_CLAW_MARKER, true);
            }
        }

        if (!holdingDualCrabClaw) {
            if (wasHoldingDualCrabClaw) {
                customData.remove(DUAL_CRAB_CLAW_MARKER);
            }
        } else {
            if (!wasHoldingDualCrabClaw) {
                customData.putBoolean(DUAL_CRAB_CLAW_MARKER, true);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(CommonComponents.EMPTY);
        tooltipComponents.add(Component.translatable("item.modifiers.hand").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
            "attribute.modifier.plus." + RANGE_ATTRIBUTE_MODIFIER.operation().id(),
            ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(RANGE_ATTRIBUTE_MODIFIER.amount()),
            Component.translatable(Attributes.BLOCK_INTERACTION_RANGE.value().getDescriptionId())
        ).withStyle(Attributes.BLOCK_INTERACTION_RANGE.value().getStyle(true)));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
