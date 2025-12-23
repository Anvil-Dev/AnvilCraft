package dev.dubhe.anvilcraft.api.sc.upgrade.level;

import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

public interface IUpgradeLevel<T extends Enum<T> & IUpgradeLevel<T>> extends StringRepresentable {
    List<ItemStack> getMaterial();

    boolean isMaterial(ItemStack material);

    List<ItemStack> getTool();

    boolean isTool(ItemStack tool);

    int getConsumedCount();

    int ordinal();

    @UnknownNullability
    T prev();

    @UnknownNullability
    T next();

    T min();

    T max();

    static boolean isAnvilHammer(ItemStack stack, @Nullable TagKey<Block> anvilTag) {
        return stack.getItem() instanceof AnvilHammerItem hammer
               && hammer.getAnvil().defaultBlockState().is(anvilTag);
    }

    String getTypeId();

    default Component getTypeName() {
        return Component.translatable("screen.anvilcraft.shulker_container.upgrade." + this.getTypeId() + ".name");
    }

    default Component getDesc() {
        return Component.translatable("screen.anvilcraft.shulker_container.upgrade." + this.getTypeId() + "." + this.getSerializedName());
    }
}
