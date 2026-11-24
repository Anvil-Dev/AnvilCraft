package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jspecify.annotations.Nullable;
import snownee.jade.addon.universal.ItemCollector;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.util.JadeForgeUtils;

import java.util.ArrayList;
import java.util.List;

/** 从大型炼药锅任意部件读取完整库存，沿用 Jade 对物品的过滤、合并与排序。 */
public enum CauldronItemStorage implements IServerExtensionProvider<ItemStack>, IClientExtensionProvider<ItemStack, ItemView> {
    INSTANCE;

    // Jade 按标识符共享提供器优先级，库存选择和预览行的绘制顺序需要独立配置。
    public static final ResourceLocation UID = AnvilCraft.of("cauldron_item_storage");
    private static final String COUNTS = "Counts";

    @Override
    public @Nullable List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)) {
            return null;
        }
        IItemHandler inventory;
        if (block.getBlockEntity() instanceof LargeCauldronBlockEntity pot) {
            inventory = new CombinedInvWrapper(pot.getInputHandler(), pot.getOutputHandler());
        } else if (block.getBlockEntity() instanceof FishTankBlockEntity fish) {
            inventory = fish.getItemHandler();
        } else {
            return null;
        }
        var groups = new ItemCollector<>(JadeForgeUtils.fromItemHandler(inventory, 0, ignored -> inventory)).update(accessor);
        if (groups != null) {
            for (ViewGroup<ItemStack> group : groups) {
                group.getExtraData().putIntArray(COUNTS, group.views.stream().mapToInt(ItemStack::getCount).toArray());
                group.views = group.views.stream().map(stack -> stack.copyWithCount(1)).toList();
            }
        }
        return groups;
    }

    @Override
    public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
        List<ClientViewGroup<ItemView>> result = new ArrayList<>();
        for (var group : groups) {
            int[] counts = group.getExtraData().getIntArray(COUNTS);
            List<ItemView> items = new ArrayList<>();
            for (int index = 0; index < group.views.size(); index++) {
                ItemStack stack = group.views.get(index);
                items.add(new ItemView(index < counts.length ? stack.copyWithCount(counts[index]) : stack));
            }
            result.add(new ClientViewGroup<>(items));
        }
        return result;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
