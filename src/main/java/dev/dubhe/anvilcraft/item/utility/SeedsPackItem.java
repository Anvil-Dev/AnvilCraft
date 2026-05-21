package dev.dubhe.anvilcraft.item.utility;

import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.stream.Collectors;

public class SeedsPackItem extends Item {
    public SeedsPackItem(Properties properties) {
        super(properties);
    }

    private List<Item> items = List.of();
    private Level level = null;

    @Override
    public InteractionResult use(
        Level level, Player player, InteractionHand usedHand
    ) {
        if (this.items.isEmpty() || this.level == null || this.level != level) {
            this.items = BuiltInRegistries.ITEM.getOrThrow(ModItemTags.SEEDS_PACK_CONTENT)
                .stream()
                .filter(Holder::isBound)
                .map(Holder::value)
                .collect(Collectors.toSet())
                .stream()
                .toList();
            this.level = level;
        }
        if (this.items.isEmpty()) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            RandomSource random = level.getRandom();
            player.getInventory().placeItemBackInInventory(new ItemStack(this.items.get(random.nextInt(this.items.size()))));
        }
        ItemStack stack = player.getItemInHand(usedHand);
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
