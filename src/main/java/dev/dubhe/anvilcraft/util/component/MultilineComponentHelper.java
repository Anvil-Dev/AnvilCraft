package dev.dubhe.anvilcraft.util.component;

import dev.dubhe.anvilcraft.util.CollectionUtil;
import dev.dubhe.anvilcraft.util.ComponentUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Collection;
import java.util.function.Function;

public class MultilineComponentHelper {
    private final Component lf;
    private final Component tab;
    private final Component splitter;
    private final Component listHead;
    private final Component listTail;
    private final Component itemHead;
    private final Component itemTail;
    private MutableComponent msg;
    private int depth = 0;

    public MultilineComponentHelper(
        Component lf,
        Component tab, Component splitter,
        Component listHead,
        Component listTail,
        Component itemHead,
        Component itemTail
    ) {
        this.lf = lf;
        this.tab = tab;
        this.splitter = splitter;
        this.listHead = listHead;
        this.listTail = listTail;
        this.itemHead = itemHead;
        this.itemTail = itemTail;
    }

    public static MultilineComponentHelper create() {
        return new MultilineComponentHelper(
            ComponentUtil.LF,
            ComponentUtil.TAB,
            ComponentUtil.SPLITTER,
            ComponentUtil.LIST_HEAD,
            ComponentUtil.LIST_TAIL,
            ComponentUtil.ITEM_HEAD,
            ComponentUtil.ITEM_TAIL
        );
    }

    public MultilineComponentHelper addln(Component c) {
        if (this.msg == null) {
            this.msg = c.copy();
            return this;
        }
        this.msg.append(this.lf);
        for (int i = 0; i < this.depth; i++) {
            this.msg.append(this.tab);
        }
        this.msg.append(c);
        return this;
    }

    public MultilineComponentHelper addln(String value) {
        return this.addln(Component.literal(value));
    }

    public MultilineComponentHelper addln(String transKey, Object... args) {
        return this.addln(Component.translatable(transKey, ComponentUtil.argValidate(args)));
    }

    public MultilineComponentHelper append(Component c) {
        if (this.msg == null) {
            this.msg = c.copy();
            return this;
        }
        this.msg.append(c);
        return this;
    }

    public MultilineComponentHelper in() {
        this.depth++;
        return this;
    }

    public MultilineComponentHelper out() {
        this.depth--;
        return this;
    }

    public <T> MultilineComponentHelper list(
        Component head,
        Collection<T> collection,
        Function<T, IComponentInfo[]> infoGetter
    ) {
        this.addln(head);
        if (collection.isEmpty()) {
            return this.append(this.listHead).append(this.listTail);
        } else if (collection.size() == 1) {
            this.append(this.itemHead);
            this.in();
            for (var info : infoGetter.apply(CollectionUtil.get(collection, 0))) {
                info.addInto(this);
            }
            this.out();
            return this.addln(this.itemTail);
        } else {
            this.append(this.listHead);
            this.in();
            for (T t : collection) {
                this.addln(this.itemHead);
                this.in();
                for (var info : infoGetter.apply(t)) {
                    info.addInto(this);
                }
                this.out();
                this.addln(this.itemTail.copy().append(this.splitter));
            }
            this.out();
            return this.addln(this.listTail);
        }
    }

    public MultilineComponentHelper enchantments(Component head, ItemEnchantments enchantments) {
        this.addln(head);
        if (enchantments.isEmpty()) {
            return this.append(this.listHead).append(this.listTail);
        } else {
            this.append(this.listHead);
            for (var entry : enchantments.entrySet()) {
                this.addln(Enchantment.getFullname(entry.getKey(), entry.getIntValue())).append(this.splitter);
            }
            return this.addln(this.listTail);
        }
    }

    public Component build() {
        return this.msg;
    }
}
