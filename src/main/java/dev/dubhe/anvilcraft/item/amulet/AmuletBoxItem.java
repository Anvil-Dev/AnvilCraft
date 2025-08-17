package dev.dubhe.anvilcraft.item.amulet;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.property.BoxContents;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AmuletBoxItem extends Item {
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;
    public static final int CAPACITY = 16;

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
            playRemoveOneSound(player);
        } else {
            if (!mutable.tryInsert(other)) {
                return false;
            }
            playInsertSound(player);
            slot.set(ItemStack.EMPTY);
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
            playRemoveOneSound(player);
            broadcastChangesOnContainerMenu(player);
        } else {
            if (!contents.tryInsert(other)) return false;
            playInsertSound(player);
            broadcastChangesOnContainerMenu(player);
            slotAccess.set(ItemStack.EMPTY);
        }
        box.set(ModComponents.BOX_CONTENTS, contents.immutable());
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        Inventory inventory = player.getInventory();
        ItemStack box = player.getItemInHand(usedHand);
        if (!level.isClientSide) {
            BoxContents contents = box.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
            BoxContents.Mutable mutable = contents.mutable();
            if (!player.isShiftKeyDown()) {
                List<ItemStack> items = InventoryUtil.getItems(inventory);
                for (ItemStack stack : items) {
                    if (stack.isEmpty() || !stack.is(ModItemTags.TOTEM)) continue;
                    if (mutable.tryInsert(stack.copy())) {
                        inventory.removeItem(stack);
                    }
                }
                playInsertSound(player);
                box.set(ModComponents.BOX_CONTENTS, mutable.immutable());
            } else if (AnvilCraft.config.amuletBoxTakeOutAllTotem) {
                boolean dropped = false;
                for (int i = 0; i < contents.getTotems().size(); i++) {
                    ItemStack stack = mutable.popTotem();
                    if (stack.isEmpty()) break;
                    player.getInventory().placeItemBackInInventory(stack);
                    dropped = true;
                }
                if (dropped) {
                    playDropContentsSound(level, player);
                }
                box.set(ModComponents.BOX_CONTENTS, mutable.immutable());
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.success(box);
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public boolean isBarVisible(ItemStack itemStack) {
        BoxContents contents = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
        return contents.getUsage() > 0;
    }

    @Override
    public int getBarWidth(ItemStack itemStack) {
        BoxContents contents = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
        return (int) (Math.clamp(contents.getUsage() / (float) CAPACITY, 0f, 1f) * 13);
    }

    @Override
    public int getBarColor(ItemStack itemStack) {
        BoxContents contents = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
        return lerpColor(contents.getUsage() / (float) CAPACITY, BAR_COLOR, FULL_BAR_COLOR);
    }

    private int lerpColor(float ratio, int from, int to) {
        int r1 = FastColor.ARGB32.red(from);
        int g1 = FastColor.ARGB32.green(from);
        int b1 = FastColor.ARGB32.blue(from);
        int r2 = FastColor.ARGB32.red(to);
        int g2 = FastColor.ARGB32.green(to);
        int b2 = FastColor.ARGB32.blue(to);
        return FastColor.ARGB32.color(
            255,
            (int) Mth.lerp(ratio, r1, r2),
            (int) Mth.lerp(ratio, g1, g2),
            (int) Mth.lerp(ratio, b1, b2)
        );
    }

    @Override
    public UseAnim getUseAnimation(ItemStack itemStack) {
        return UseAnim.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        BoxContents contents = stack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.translatable(
                "tooltip.anvilcraft.press_key",
                Component.literal("Shift").withStyle(ChatFormatting.WHITE)
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("tooltip.anvilcraft.item.amulet_box.line_1").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("tooltip.anvilcraft.item.amulet_box.line_2").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.anvilcraft.press_key", Component.literal("Shift")));
        }
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable(
            "tooltip.anvilcraft.item.amulet_box.fullness", contents.getUsage(), CAPACITY
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity, DamageSource source) {
        BoxContents contents = itemEntity.getItem().get(ModComponents.BOX_CONTENTS);
        if (contents != null) {
            itemEntity.getItem().set(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
            ItemUtils.onContainerDestroyed(itemEntity, contents.allItems());
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
