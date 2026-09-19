package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import lombok.Getter;
import net.minecraft.core.component.DataComponentType;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;

import javax.annotation.Nullable;

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

    public static @Nullable DataComponentType<Boolean> abilityComponent(ItemStack stack) {
        if (stack.is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) return ModComponents.NIGHT_VISION_ENABLED;
        if (stack.is(ModItems.BUFFER_BOOTS) || stack.is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS)) {
            return ModComponents.CHARGED_JUMP_ENABLED;
        }
        return null;
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
