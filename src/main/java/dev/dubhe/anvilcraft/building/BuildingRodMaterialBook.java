package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

final class BuildingRodMaterialBook {
    private BuildingRodMaterialBook() {
    }

    static void give(ServerPlayer player, List<Component> missing) {
        if (missing.isEmpty()) return;
        ItemStack book = PocketInventory.carriedItems(player).stream().filter(stack -> stack.is(Items.BOOK))
            .findFirst().orElse(ItemStack.EMPTY);
        if (book.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.anvilcraft.building_rod.missing_book"), false);
            return;
        }
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int start = 0; start < missing.size(); start += 6) {
            var page = Component.empty();
            for (int line = start; line < Math.min(start + 6, missing.size()); line++) {
                page.append(missing.get(line)).append("\n");
            }
            pages.add(Filterable.passThrough(page));
        }
        ItemStack result = new ItemStack(Items.WRITTEN_BOOK);
        result.set(DataComponents.WRITTEN_BOOK_CONTENT,
            new WrittenBookContent(Filterable.passThrough("Material List"), "", 0, pages, false));
        result.set(DataComponents.ITEM_NAME, Component.translatable("book.anvilcraft.material_list.title"));
        book.shrink(1);
        player.getInventory().placeItemBackInInventory(result);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }
}
