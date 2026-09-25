package dev.dubhe.anvilcraft.api.event;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 从引物获取自选附魔事件。
 * 取消事件则引物不提供任何附魔。
 */
@Getter
public class PrimerEnchantmentsEvent extends Event implements ICancellableEvent {
    private final Level level;
    private final ItemStack primer;
    private final List<Holder<Enchantment>> enchantments = new ArrayList<>();

    public PrimerEnchantmentsEvent(Level level, ItemStack primer) {
        this.level = level;
        this.primer = primer;
    }

    public void addEnchantment(Holder<Enchantment> enchantment) {
        if (!this.enchantments.contains(enchantment)) {
            this.enchantments.add(enchantment);
        }
    }

    public void addEnchantments(Collection<Holder<Enchantment>> enchantments) {
        for (Holder<Enchantment> enchantment : enchantments) {
            this.addEnchantment(enchantment);
        }
    }

    public List<Holder<Enchantment>> getEnchantments() {
        return Collections.unmodifiableList(this.enchantments);
    }
}
