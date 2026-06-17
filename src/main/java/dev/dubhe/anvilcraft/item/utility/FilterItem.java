package dev.dubhe.anvilcraft.item.utility;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.FilterMenu;
import dev.dubhe.anvilcraft.inventory.container.FilterContainer;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FilterItem extends Item {
    public FilterItem(Properties properties) {
        super(properties.component(ModComponents.FILTER_CONTENT, new FilterContent()));
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
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (!itemstack.is(ModItems.FILTER)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int position = usedHand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : 151;
        ModMenuTypes.open((ServerPlayer) player, new FilterMenuProvider(position));
        return InteractionResult.SUCCESS;
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
                new FilterContainer(player, this.position, player.getInventory().getItem(this.position))
            );
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            if (!(menu instanceof FilterMenu filterMenu)) return;
            buffer.writeInt(filterMenu.getContainer().getPosition());
        }
    }
}
