package dev.dubhe.anvilcraft.item.armor;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

public class EquipmentArmorItem extends Item {
    private final ArmorType armorType;
    private final Identifier texture;

    public EquipmentArmorItem(Properties properties, ArmorType type, boolean weatherproof, String texture) {
        super(properties(properties, type, weatherproof, texture));
        this.armorType = type;
        this.texture = AnvilCraft.of("textures/entity/equipment/" + texture + ".png");
    }

    private static Properties properties(Properties properties, ArmorType type, boolean weatherproof, String texture) {
        var material = weatherproof ? ArmorMaterials.NETHERITE : ArmorMaterials.IRON;
        properties.humanoidArmor(material, type)
            .durability(type.getDurability(weatherproof ? 74 : 15))
            .repairable(weatherproof ? ModItemTags.WEATHERPROOF_REPAIR_MATERIALS : Tags.Items.INGOTS_IRON)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(type.getSlot()).setEquipSound(material.equipSound())
                .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, AnvilCraft.of(texture))).build());
        if (weatherproof) {
            properties.repairable(ModItemTags.WEATHERPROOF_REPAIR_MATERIALS);
            Identifier id = Identifier.withDefaultNamespace("armor." + type.getName());
            EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
            int defense = material.defense().get(type) + (type == ArmorType.HELMET || type == ArmorType.BOOTS ? 2 : 3);
            float toughness = material.toughness() + (type == ArmorType.CHESTPLATE ? 2 : 1);
            properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(id, defense, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, toughness, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, 0.1, AttributeModifier.Operation.ADD_VALUE), slot)
                .build());
        }
        if (!weatherproof) properties.repairable(Items.IRON_INGOT);
        return properties;
    }

    public EquipmentSlot getEquipmentSlot() {
        return this.armorType.getSlot();
    }

    public Identifier getArmorTexture() {
        return this.texture;
    }

    public static @Nullable DataComponentType<Boolean> abilityComponent(ItemStack stack) {
        if (stack.is(ModItems.BUFFER_BOOTS) || stack.is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS)) return ModComponents.CHARGED_JUMP_ENABLED;
        return stack.is(ModItems.WEATHERPROOF_SPACESUIT_HELMET) ? ModComponents.NIGHT_VISION_ENABLED : null;
    }
}
