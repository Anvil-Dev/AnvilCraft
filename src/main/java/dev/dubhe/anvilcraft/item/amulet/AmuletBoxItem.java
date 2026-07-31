package dev.dubhe.anvilcraft.item.amulet;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class AmuletBoxItem extends Item {
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;

    public AmuletBoxItem(Properties properties) {
        super(properties.component(ModComponents.BOX_CONTENTS, BoxContents.EMPTY));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack itemStack, Slot slot, ClickAction clickAction, Player player) {
        if (clickAction != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        BoxContents contents = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
        BoxContents.Mutable mutable = contents.mutable();
        ItemStack other = slot.getItem();
        if (other.isEmpty()) {
            ItemStack popped = mutable.pop();
            if (popped.isEmpty()) return false;
            slot.set(popped);
            AmuletBoxItem.playRemoveOneSound(player);
        } else {
            Optional<ItemStack> remain = mutable.tryInsert(other);
            if (remain.isEmpty()) return false;
            AmuletBoxItem.playInsertSound(player);
            slot.set(remain.get());
        }
        itemStack.set(ModComponents.BOX_CONTENTS, mutable.immutable());
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
        ItemStack box,
        ItemStack other,
        Slot slot,
        ClickAction clickAction,
        Player player,
        SlotAccess slotAccess
    ) {
        if (clickAction != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        BoxContents.Mutable contents = box.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).mutable();
        if (other.isEmpty()) {
            ItemStack itemStack = contents.pop();
            if (itemStack.isEmpty()) return false;
            slotAccess.set(itemStack);
            AmuletBoxItem.playRemoveOneSound(player);
            this.broadcastChangesOnContainerMenu(player);
        } else {
            Optional<ItemStack> remain = contents.tryInsert(other);
            if (remain.isEmpty()) return false;
            AmuletBoxItem.playInsertSound(player);
            this.broadcastChangesOnContainerMenu(player);
            slotAccess.set(remain.get());
        }
        box.set(ModComponents.BOX_CONTENTS, contents.immutable());
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        Inventory inventory = player.getInventory();
        ItemStack box = player.getItemInHand(usedHand);
        if (!level.isClientSide()) {
            BoxContents contents = box.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
            BoxContents.Mutable mutable = contents.mutable();
            if (!player.isShiftKeyDown()) {
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.isEmpty() || !stack.is(ModItemTags.TOTEM)) continue;
                    Optional<ItemStack> remain = mutable.tryInsert(stack.copy());
                    if (remain.isEmpty()) continue;
                    inventory.setItem(i, remain.get());
                }
                AmuletBoxItem.playInsertSound(player);
                box.set(ModComponents.BOX_CONTENTS, mutable.immutable());
            } else if (AnvilCraft.CONFIG.amuletBoxTakeOutAllTotem) {
                boolean dropped = false;
                for (int i = 0; i < contents.totems().size(); i++) {
                    ItemStack stack = mutable.popTotem();
                    if (stack.isEmpty()) break;
                    player.getInventory().placeItemBackInInventory(stack);
                    dropped = true;
                }
                if (dropped) {
                    AmuletBoxItem.playDropContentsSound(level, player);
                }
                box.set(ModComponents.BOX_CONTENTS, mutable.immutable());
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public boolean isBarVisible(ItemStack itemStack) {
        BoxContents contents = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
        return contents.usage() > 0;
    }

    @Override
    public int getBarWidth(ItemStack itemStack) {
        BoxContents contents = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
        return (int) (Math.clamp(contents.usage() / (float) BoxContents.CAPACITY, 0F, 1F) * 13);
    }

    @Override
    public int getBarColor(ItemStack itemStack) {
        BoxContents contents = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
        return ColorUtil.lerpColor(contents.usage() / (float) BoxContents.CAPACITY, AmuletBoxItem.BAR_COLOR, AmuletBoxItem.FULL_BAR_COLOR);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity, DamageSource source) {
        BoxContents contents = itemEntity.getItem().get(ModComponents.BOX_CONTENTS);
        if (contents != null) {
            itemEntity.getItem().set(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
            ItemUtils.onContainerDestroyed(itemEntity, contents.allItems().stream());
        }
    }

    private static void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playDropContentsSound(Level level, Entity entity) {
        level.playSound(
            null,
            entity.blockPosition(),
            SoundEvents.BUNDLE_DROP_CONTENTS,
            SoundSource.PLAYERS,
            0.8F,
            0.8F + entity.level().getRandom().nextFloat() * 0.4F
        );
    }

    private void broadcastChangesOnContainerMenu(Player player) {
        player.containerMenu.slotsChanged(player.getInventory());
    }
}
