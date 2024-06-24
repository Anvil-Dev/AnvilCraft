package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModItemTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SeedsPackItem extends Item {
    public SeedsPackItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand
    ) {
        ItemStack stack = player.getItemInHand(usedHand);
        List<Item> items = BuiltInRegistries.ITEM.getOrCreateTag(ModItemTags.SEEDS_PACK_CONTENT)
            .stream()
            .filter(Holder::isBound)
            .map(Holder::value)
            .toList();
        if (items.isEmpty()) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide()) {
            RandomSource random = level.getRandom();
            player.getInventory().placeItemBackInInventory(new ItemStack(items.get(random.nextInt(items.size()))));
        }
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }
}
