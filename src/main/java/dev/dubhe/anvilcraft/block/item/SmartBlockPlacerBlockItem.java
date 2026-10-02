package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class SmartBlockPlacerBlockItem extends BlockItem {
    public SmartBlockPlacerBlockItem(Block block, Properties properties) {
        super(block, properties
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                .setEquipSound(ArmorMaterials.IRON.equipSound()).build())
            .attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(
                    AnvilCraft.of("smart_block_placer_helmet"), ArmorMaterials.IRON.defense().get(ArmorType.HELMET),
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                .build()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.canUseSlot(EquipmentSlot.HEAD) || !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return InteractionResult.FAIL;
        }
        player.setItemSlot(EquipmentSlot.HEAD, stack.copyWithCount(1));
        stack.consume(1, player);
        if (!level.isClientSide()) player.awardStat(Stats.ITEM_USED.get(this));
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}
