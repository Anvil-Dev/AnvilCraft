package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public class EquipmentArmorItem extends ArmorItem {
    @Getter
    private final boolean weatherproof;
    private final ResourceLocation texture;

    public EquipmentArmorItem(Properties properties, Type type, boolean weatherproof, String texture) {
        super(weatherproof ? ArmorMaterials.NETHERITE : ArmorMaterials.IRON, type,
            properties.durability(type.getDurability(weatherproof ? 74 : 15)));
        this.weatherproof = weatherproof;
        this.texture = AnvilCraft.of("textures/entity/equipment/" + texture + ".png");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        switch (this.type) {
            case HELMET -> {
                TooltipUtil.addTranslatedLines(tooltip, ChatFormatting.GRAY, "tooltip.anvilcraft.equipment.breathing");
                if (this.weatherproof) {
                    TooltipUtil.addTranslatedLines(tooltip, ChatFormatting.GRAY, "tooltip.anvilcraft.equipment.clear_vision");
                }
            }
            case CHESTPLATE -> {
                if (this.weatherproof) {
                    TooltipUtil.addTranslatedLines(tooltip, ChatFormatting.GRAY, "tooltip.anvilcraft.equipment.recharge");
                }
            }
            case LEGGINGS -> TooltipUtil.addTranslatedLines(
                tooltip, ChatFormatting.GRAY, "tooltip.anvilcraft.equipment.pockets", this.weatherproof ? 12 : 6);
            case BOOTS -> {
                TooltipUtil.addTranslatedLines(tooltip, ChatFormatting.GRAY, "tooltip.anvilcraft.equipment.buffer_boots");
                if (this.weatherproof) {
                    TooltipUtil.addTranslatedLines(tooltip, ChatFormatting.GRAY, "tooltip.anvilcraft.equipment.fluid_walking");
                }
            }
            default -> {
            }
        }
        if (this.weatherproof) {
            TooltipUtil.addTranslatedLines(tooltip, ChatFormatting.AQUA, "tooltip.anvilcraft.equipment.full_suit");
        }
    }

    @Override
    public int getDefense() {
        return super.getDefense() + (this.weatherproof ? (this.type == Type.HELMET || this.type == Type.BOOTS ? 2 : 3) : 0);
    }

    @Override
    public float getToughness() {
        return super.getToughness() + (this.weatherproof ? (this.type == Type.CHESTPLATE ? 2 : 1) : 0);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        if (!this.weatherproof) return super.getDefaultAttributeModifiers();
        ResourceLocation id = ResourceLocation.withDefaultNamespace("armor." + this.type.getName());
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(this.type.getSlot());
        return ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR, new AttributeModifier(id, this.getDefense(), AttributeModifier.Operation.ADD_VALUE), slot)
            .add(Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(id, this.getToughness(), AttributeModifier.Operation.ADD_VALUE), slot)
            .add(Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(id, 0.1, AttributeModifier.Operation.ADD_VALUE), slot)
            .build();
    }

    @Override
    public boolean isValidRepairItem(ItemStack armor, ItemStack repair) {
        return this.weatherproof ? repair.is(ModItems.MULTIPHASE_MATTER) : repair.is(Items.IRON_INGOT);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                           ArmorMaterial.Layer layer, boolean innerModel) {
        return this.texture;
    }
}
