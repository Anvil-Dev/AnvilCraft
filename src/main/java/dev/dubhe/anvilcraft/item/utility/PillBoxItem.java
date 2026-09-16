package dev.dubhe.anvilcraft.item.utility;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.BundleLikeItem;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PillBoxItem extends BundleLikeItem {
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
        return PillBoxItem.use(itemStack, player);
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
        player.getCooldowns().addCooldown(pillBox, 40);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean storesContentsLocally() {
        return true;
    }

    @Override
    protected void removeOne(TransferState state) {
        ItemStack stack = state.getStack();
        var contents = stack.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY).mutable();
        state.setOutput(contents.get().orElse(null));
        stack.set(ModComponents.PILL_BOX_CONTENTS, contents.immutable());
    }

    @Override
    protected void insertOne(TransferState state) {
        ItemStack stack = state.getStack();
        var contents = stack.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY).mutable();
        state.setOutput(contents.insert(state.getOther()) ? ItemStack.EMPTY : null);
        stack.set(ModComponents.PILL_BOX_CONTENTS, contents.immutable());
    }

    @Override
    protected void updateStack(ItemStack stack, TransferState state) {
        stack.set(ModComponents.PILL_BOX_CONTENTS, state.getStack().get(ModComponents.PILL_BOX_CONTENTS));
    }
}
