package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.property.FilterContent;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.FilterMenu;
import dev.dubhe.anvilcraft.inventory.container.FilterContainer;
import lombok.AllArgsConstructor;
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
import net.minecraft.world.level.Level;

public class FilterItem extends Item {
    public FilterItem(Properties properties) {
        super(properties);
    }

    public static boolean filter(ItemStack filterStack, ItemStack stack) {
        return FilterContent.filter(filterStack, stack, false, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (!itemstack.is(ModItems.FILTER)) return InteractionResultHolder.pass(itemstack);
        if (level.isClientSide()) return InteractionResultHolder.success(itemstack);
        if (!itemstack.has(ModComponents.FILTER_CONTENT)) {
            itemstack.set(ModComponents.FILTER_CONTENT, new FilterContent());
        }
        int position = usedHand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 151;
        ModMenuTypes.open((ServerPlayer) player, new FilterMenuProvider(position));
        return InteractionResultHolder.success(itemstack);
    }

    @AllArgsConstructor
    public static final class FilterMenuProvider implements MenuProvider {
        private final int position;

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
