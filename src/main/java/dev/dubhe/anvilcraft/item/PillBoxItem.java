package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PillBoxItem extends BundleLikeItem {
    public PillBoxItem(Properties properties) {
        super(properties.component(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY));
    }

    @Override
    protected void removeOne(TransferState state) {
        ItemStack stack = state.getStack();
        PillBoxContents.Mutable mutable = stack.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY).mutable();
        state.setOutput(mutable.get().orElse(null));
        stack.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
    }

    @Override
    protected void insertOne(TransferState state) {
        ItemStack stack = state.getStack();
        PillBoxContents.Mutable mutable = stack.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY).mutable();
        if (mutable.insert(state.getOther())) {
            state.setOutput(ItemStack.EMPTY);
        } else {
            state.setOutput(null);
        }
        stack.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
    }

    @Override
    protected void updateStack(ItemStack stack, TransferState state) {
        stack.set(ModComponents.PILL_BOX_CONTENTS, state.getStack().get(ModComponents.PILL_BOX_CONTENTS));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        return use(itemStack, player);
    }

    public static InteractionResultHolder<ItemStack> use(ItemStack pillBox, Player player) {
        if (!pillBox.is(ModItems.PILL_BOX)) {
            return InteractionResultHolder.pass(pillBox);
        }
        PillBoxContents contents = pillBox.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY);
        if (contents.pills().isEmpty()) {
            return InteractionResultHolder.pass(pillBox);
        }
        PillBoxContents.Mutable mutable = contents.mutable();
        mutable.useAll(player);
        pillBox.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
        player.getCooldowns().addCooldown(ModItems.PILL_BOX.asItem(), 40);
        return InteractionResultHolder.success(pillBox);
    }
}
