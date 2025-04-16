package dev.dubhe.anvilcraft.item.amulet;

import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AmuletBoxItem extends Item {
    public AmuletBoxItem(Properties properties) {
        super(properties.component(ModComponents.TOTEM_COUNT, 0));
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        Inventory inventory = player.getInventory();
        ItemStack box = InventoryUtil.getFirstItem(inventory, this);
        if (!level.isClientSide) {
            if (!player.isShiftKeyDown()) {
                while (
                    !this.isFullTotem(box)
                    && inventory.contains(stack -> stack.getItem().equals(Items.TOTEM_OF_UNDYING))
                ) {
                    ItemStack totem = InventoryUtil.getFirstItem(inventory, Items.TOTEM_OF_UNDYING);
                    box.set(ModComponents.TOTEM_COUNT, box.get(ModComponents.TOTEM_COUNT) + 1);
                    totem.shrink(1);
                }
            } else {
                for (int i = box.get(ModComponents.TOTEM_COUNT); i > 0; i--) {
                    InventoryUtil.addToInventory(player.getInventory(), Items.TOTEM_OF_UNDYING.getDefaultInstance());
                }
                box.set(ModComponents.TOTEM_COUNT, 0);
            }
            return InteractionResultHolder.success(box);
        }
        return super.use(level, player, usedHand);
    }

    @SuppressWarnings("DataFlowIssue")
    private boolean isFullTotem(ItemStack stack) {
        return !(stack.getItem() instanceof AmuletBoxItem) || stack.get(ModComponents.TOTEM_COUNT) >= this.getMaxTotemCount();
    }

    public int getMaxTotemCount() {
        return 16;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.anvilcraft.item.amulet_box.line_1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.anvilcraft.item.amulet_box.line_2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable(
            "tooltip.anvilcraft.item.amulet_box.totem_count",
            stack.get(ModComponents.TOTEM_COUNT)
        ).withStyle(ChatFormatting.GRAY));
    }
}
