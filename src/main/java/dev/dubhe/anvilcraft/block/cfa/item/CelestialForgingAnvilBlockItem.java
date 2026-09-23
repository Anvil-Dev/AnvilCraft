package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.item.block.SimpleMultiPartBlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.Equippable;

public class CelestialForgingAnvilBlockItem extends SimpleMultiPartBlockItem<Cube323PartHalf> {
    public static final String ITEM_RENDER_DATA = "itemRenderData";

    public CelestialForgingAnvilBlockItem(SimpleMultiPartBlock<Cube323PartHalf> block, Properties properties) {
        super(block, equipment(properties));
    }

    private static Properties equipment(Properties properties) {
        var id = AnvilCraft.of("celestial_forging_anvil_helmet");
        return properties.enchantable(ArmorMaterials.NETHERITE.enchantmentValue())
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                .setEquipSound(ArmorMaterials.NETHERITE.equipSound()).build())
            .attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(id, 5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, 4, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD)
                .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, 0.1, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD)
                .build());
    }

    public static void saveRenderData(CompoundTag blockEntityTag, long gameTime) {
        CompoundTag renderData = new CompoundTag();
        for (String key : blockEntityTag.keySet()) {
            if (key.startsWith("activeMegastructure") || key.startsWith("stellar") || key.startsWith("accelerator")
                || key.equals("excavatorLaserActive") || key.equals("penroseSphereLaserActive")) {
                var value = blockEntityTag.get(key);
                if (value != null) renderData.put(key, value.copy());
            }
        }
        if (renderData.getLongOr("acceleratorPausedSinceGameTime", -1) < 0) {
            renderData.putLong("acceleratorPausedSinceGameTime", gameTime);
        }
        blockEntityTag.put(ITEM_RENDER_DATA, renderData);
    }
}
