package dev.dubhe.anvilcraft.util.component;

import dev.dubhe.anvilcraft.util.ComponentUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record TranslatableInfo(String key, Object... args) implements IComponentInfo {
    @Override
    public void addInto(MultilineComponentHelper helper) {
        var head = Component.translatable(this.key);
        if (this.args.length == 0) {
            helper.addln(head);
            return;
        }

        List<Component> args = new ArrayList<>();
        for (int i = 0, objectsLength = this.args.length; i < objectsLength; i++) {
            Object arg = this.args[i];
            switch (arg) {
                case Collection<?> collection -> helper.list(head, collection, Util.cast(this.args[++i]));
                case ItemEnchantments enchantments -> helper.enchantments(head, enchantments);
                default -> args.add(ComponentUtil.argValidate(arg));
            }
        }
        if (args.isEmpty()) return;
        helper.addln(head);
        for (Component arg : args) {
            helper.append(arg);
        }
    }
}
