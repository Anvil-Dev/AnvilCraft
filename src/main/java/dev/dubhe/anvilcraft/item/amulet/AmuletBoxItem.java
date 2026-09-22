package dev.dubhe.anvilcraft.item.amulet;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.item.BundleLikeItem;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.consume.PreventShrinkingConsumeEffect;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.Optional;

public class AmuletBoxItem extends BundleLikeItem {
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;

    public AmuletBoxItem(Properties properties) {
        super(properties.component(ModComponents.BOX_CONTENTS, BoxContents.EMPTY));
    }

    @Override
    protected boolean storesContentsLocally() {
        return true;
    }

    @Override
    protected void removeOne(TransferState state) {
        ItemStack stack = state.getStack();
        var contents = stack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).mutable();
        state.setOutput(contents.pop());
        stack.set(ModComponents.BOX_CONTENTS, contents.immutable());
    }

    @Override
    protected void insertOne(TransferState state) {
        ItemStack stack = state.getStack();
        var contents = stack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).mutable();
        state.setOutput(contents.tryInsert(state.getOther()).orElse(null));
        stack.set(ModComponents.BOX_CONTENTS, contents.immutable());
    }

    @Override
    protected void updateStack(ItemStack stack, TransferState state) {
        stack.set(ModComponents.BOX_CONTENTS, state.getStack().get(ModComponents.BOX_CONTENTS));
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
                BundleLikeItem.playSound(player, SoundEvents.BUNDLE_INSERT);
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

    public static boolean tryUsePocketTotem(ServerPlayer player, DamageSource damage) {
        if (damage.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        PocketInventory pockets = PocketInventory.get(player);
        for (int slot = 0; slot < PocketInventory.capacity(player); slot++) {
            ItemStack box = pockets.getItem(slot);
            if (!box.is(ModItems.AMULET_BOX)) continue;
            var protection = box.get(DataComponents.DEATH_PROTECTION);
            if (protection == null || !CommonHooks.onLivingUseTotem(player, damage, box, InteractionHand.OFF_HAND)) continue;
            ItemStack activated = box.copy();
            InteractionHand previous = PreventShrinkingConsumeEffect.USED_HAND.get();
            try {
                PreventShrinkingConsumeEffect.USED_HAND.remove();
                player.setHealth(1.0F);
                protection.applyEffects(activated, player);
            } finally {
                if (previous == null) PreventShrinkingConsumeEffect.USED_HAND.remove();
                else PreventShrinkingConsumeEffect.USED_HAND.set(previous);
            }
            pockets.setItem(slot, activated);
            pockets.syncChanges(player);
            player.awardStat(Stats.ITEM_USED.get(box.getItem()));
            CriteriaTriggers.USED_TOTEM.trigger(player, box);
            box.causeUseVibration(player, GameEvent.ITEM_INTERACT_FINISH);
            player.level().broadcastEntityEvent(player, (byte) 35);
            return true;
        }
        return false;
    }
}
