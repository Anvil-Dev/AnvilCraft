package dev.dubhe.anvilcraft.item.utility;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class PillBoxItem extends Item {
    public PillBoxItem(Properties properties) {
        super(
            properties
                .component(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY)
                .useCooldown(2)
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        return use(itemStack, player);
    }

    public static InteractionResult use(ItemStack pillBox, Player player) {
        if (!pillBox.is(ModItems.PILL_BOX)) {
            return InteractionResult.PASS;
        }
        PillBoxContents contents = pillBox.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY);
        if (contents.pills().isEmpty()) {
            return InteractionResult.PASS;
        }
        PillBoxContents.Mutable mutable = contents.mutable();
        mutable.useAll(player);
        pillBox.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
        ItemStack stack,
        ItemStack other,
        Slot slot,
        ClickAction action,
        Player player,
        SlotAccess access
    ) {
        final PillBoxContents contents = stack.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY);
        final PillBoxContents.Mutable mutable = contents.mutable();
        if (!slot.allowModification(player)) {
            return false;
        }
        if (action == ClickAction.PRIMARY) {
            if (!other.isEmpty()) {
                if (mutable.insert(other)) {
                    stack.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
                    access.set(ItemStack.EMPTY);
                    return true;
                }
            }
        } else if (action == ClickAction.SECONDARY) {
            if (other.isEmpty()) {
                Optional<ItemStack> stackOptional = mutable.get();
                if (stackOptional.isPresent()) {
                    ItemStack itemStack = stackOptional.get();
                    if (!itemStack.isEmpty()) {
                        access.set(itemStack);
                        stack.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        final PillBoxContents contents = stack.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY);
        final PillBoxContents.Mutable mutable = contents.mutable();
        final ItemStack other = slot.getItem();
        if (!slot.allowModification(player)) {
            return false;
        }
        if (action == ClickAction.PRIMARY) {
            if (!other.isEmpty()) {
                if (mutable.insert(other)) {
                    stack.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
                    slot.set(ItemStack.EMPTY);
                    return true;
                }
            }
        } else if (action == ClickAction.SECONDARY) {
            if (other.isEmpty()) {
                Optional<ItemStack> stackOptional = mutable.get();
                if (stackOptional.isPresent()) {
                    ItemStack itemStack = stackOptional.get();
                    if (!itemStack.isEmpty()) {
                        slot.set(itemStack);
                        stack.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
