package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

public class CelestialForgingAnvilBlockItem extends SimpleMultiPartBlockItem<Cube323PartHalf> implements Equipable {
    public static final String ITEM_RENDER_DATA = "itemRenderData";
    private final ItemAttributeModifiers armorModifiers;

    public CelestialForgingAnvilBlockItem(
        SimpleMultiPartBlock<Cube323PartHalf> block, Properties properties
    ) {
        super(block, properties);
        var material = ArmorMaterials.NETHERITE.value();
        var modifierId = AnvilCraft.of("celestial_forging_anvil_helmet");
        this.armorModifiers = ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR,
                new AttributeModifier(modifierId, material.getDefense(ArmorItem.Type.HELMET), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.HEAD)
            .add(Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(modifierId, material.toughness(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.HEAD)
            .build();
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return ArmorMaterials.NETHERITE.value().equipSound();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return this.armorModifiers;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ArmorMaterials.NETHERITE.value().enchantmentValue();
    }

    public static void saveRenderData(CompoundTag blockEntityTag, long gameTime) {
        CompoundTag renderData = new CompoundTag();
        for (String key : blockEntityTag.getAllKeys()) {
            if (key.startsWith("activeMegastructure") || key.startsWith("stellar") || key.startsWith("accelerator")
                || key.equals("excavatorLaserActive") || key.equals("penroseSphereLaserActive")) {
                var value = blockEntityTag.get(key);
                if (value != null) renderData.put(key, value.copy());
            }
        }
        if (renderData.getLong("acceleratorPausedSinceGameTime") < 0
            || !renderData.contains("acceleratorPausedSinceGameTime")) {
            renderData.putLong("acceleratorPausedSinceGameTime", gameTime);
        }
        blockEntityTag.put(ITEM_RENDER_DATA, renderData);
    }
}
