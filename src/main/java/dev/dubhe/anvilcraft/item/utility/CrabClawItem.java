package dev.dubhe.anvilcraft.item.utility;

import com.google.common.collect.Multimap;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.attribute.EntityReachAttribute;
import dev.dubhe.anvilcraft.util.EntityUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.function.Supplier;

public class CrabClawItem extends Item {

    public static final AttributeModifier RANGE_ATTRIBUTE_MODIFIER = new AttributeModifier(
        AnvilCraft.of("range_modifier"),
        3,
        AttributeModifier.Operation.ADD_VALUE
    );
    private static final Supplier<Multimap<Holder<Attribute>, AttributeModifier>> RANGE_MODIFIER_SUPPLIER =
        EntityReachAttribute.getRangeModifierSupplier(CrabClawItem.RANGE_ATTRIBUTE_MODIFIER);
    public static final String CRAB_CLAW_MARKER = "crabClaw";
    public static final String DUAL_CRAB_CLAW_MARKER = "dualCrabClaw";

    public CrabClawItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
                .add(Attributes.BLOCK_INTERACTION_RANGE, CrabClawItem.RANGE_ATTRIBUTE_MODIFIER, EquipmentSlotGroup.HAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, CrabClawItem.RANGE_ATTRIBUTE_MODIFIER, EquipmentSlotGroup.HAND)
                .build()
        ));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Shulker shulker && shulker.isAlive()) {
            if (!player.level().isClientSide()) {
                EntityUtil.setShulkerOpen(shulker);
            }

            return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.PASS;
    }
}
