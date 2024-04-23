package dev.dubhe.anvilcraft.item;

import com.google.common.collect.Multimap;
import dev.dubhe.anvilcraft.api.entity.EntityHelper;
import dev.dubhe.anvilcraft.api.entity.attribute.EntityReachAttribute;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.UUID;
import java.util.function.Supplier;

public class CrabClawItem extends Item {

    public static final AttributeModifier rangeAttributeModifier =
        new AttributeModifier(
            UUID.fromString("ea7bed72-603d-4990-87b6-24abe91ea523"),
            "RangeModifier",
            3,
            AttributeModifier.Operation.ADDITION
        );

    public static final String CRAB_CLAW_MARKER = "crabClaw";
    public static final String DUAL_CRAB_CLAW_MARKER = "dualCrabClaw";

    private static final Supplier<Multimap<Attribute, AttributeModifier>> rangeModifier
        = EntityReachAttribute.getRangeModifierSupplier(rangeAttributeModifier);

    public CrabClawItem(Properties properties) {
        super(properties);
    }

    /**
     * 蟹钳增加交换距离
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
            player.getAttributes()
                .removeAttributeModifiers(rangeModifier.get());
            if (wasHoldingCrabClaw) {
                customData.remove(CRAB_CLAW_MARKER);
            }
        } else {
            player.getAttributes()
                .addTransientAttributeModifiers(rangeModifier.get());
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

}
