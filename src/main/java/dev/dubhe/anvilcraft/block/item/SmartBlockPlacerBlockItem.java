package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class SmartBlockPlacerBlockItem extends BlockItem implements Equipable {
    private final ItemAttributeModifiers armorModifiers;

    public SmartBlockPlacerBlockItem(Block block, Properties properties) {
        super(block, properties);
        this.armorModifiers = ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR, new AttributeModifier(
                AnvilCraft.of("smart_block_placer_helmet"),
                ArmorMaterials.IRON.value().getDefense(ArmorItem.Type.HELMET),
                AttributeModifier.Operation.ADD_VALUE
            ), EquipmentSlotGroup.HEAD)
            .build();
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return ArmorMaterials.IRON.value().equipSound();
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return this.armorModifiers;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.canUseSlot(EquipmentSlot.HEAD) || !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        player.setItemSlot(EquipmentSlot.HEAD, stack.copyWithCount(1));
        stack.consume(1, player);
        if (!level.isClientSide()) player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
