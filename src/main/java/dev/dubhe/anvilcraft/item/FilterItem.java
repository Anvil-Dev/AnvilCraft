package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.FilterMenu;
import dev.dubhe.anvilcraft.inventory.container.FilterContainer;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class FilterItem extends Item {
    public FilterItem(Properties properties) {
        super(properties);
    }

    public static boolean filter(ItemStack filter, ItemStack stack) {
        return filter.isEmpty()
               || (
                   filter.is(ModItems.FILTER)
                   ? ItemStack.isSameItemSameComponents(filter, stack)
                   : ItemStack.isSameItem(filter, stack)
               )
               || FilterContent.filter(filter, stack, false);
    }

    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {
        if (stack.is(ModItems.FILTER) && !stack.has(ModComponents.FILTER_CONTENT)) {
            stack.set(ModComponents.FILTER_CONTENT, new FilterContent());
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (!itemstack.is(ModItems.FILTER)) return InteractionResultHolder.pass(itemstack);
        if (level.isClientSide()) return InteractionResultHolder.success(itemstack);
        int position = usedHand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 151;
        ModMenuTypes.open((ServerPlayer) player, new FilterMenuProvider(position));
        return InteractionResultHolder.success(itemstack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (stack.has(ModComponents.FILTER_CONTENT)) {
            FilterContent content = stack.get(ModComponents.FILTER_CONTENT);
            if (content != null) {
                // 匹配组件模式：精确匹配（青加粗）/ 仅按物品匹配（灰加粗）
                Component matchComponent = Component.translatable(
                    content.includeComponents() ? "screen.anvilcraft.filter.match_component" : "screen.anvilcraft.filter.mismatch_component"
                ).withStyle(ChatFormatting.BOLD)
                    .withStyle(content.includeComponents() ? ChatFormatting.AQUA : ChatFormatting.GRAY);
                // 白/黑名单：放行（绿加粗）/ 拒绝（红加粗）
                Component listMode = Component.translatable(
                    content.blackList() ? "screen.anvilcraft.filter.black_list" : "screen.anvilcraft.filter.white_list"
                ).withStyle(ChatFormatting.BOLD)
                    .withStyle(content.blackList() ? ChatFormatting.RED : ChatFormatting.GREEN);
                // 拆成两行显示
                tooltipComponents.add(matchComponent);
                tooltipComponents.add(listMode);
            }
        }
    }

    public record FilterMenuProvider(int position) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("item.anvilcraft.filter");
        }

        @Override
        public FilterMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new FilterMenu(
                ModMenuTypes.FILTER.get(),
                containerId,
                playerInventory,
                new FilterContainer(player, this.position, player.getInventory().getItem(position))
            );
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            if (!(menu instanceof FilterMenu filterMenu)) return;
            buffer.writeInt(filterMenu.getContainer().getPosition());
        }
    }
}
