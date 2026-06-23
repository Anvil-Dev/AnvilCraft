package dev.dubhe.anvilcraft.api.amulet.type;

import com.google.common.collect.Lists;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.fromto.Effect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Supplier;

public class FourToOneAmuletType extends AmuletType {
    private final List<AmuletType> types;

    protected FourToOneAmuletType(List<Supplier<? extends AmuletType>> types, Supplier<ItemStack> amulet) {
        super((player, source) -> false, Effect.NOP, amulet);
        this.types = Lists.transform(types.subList(0, 4), Supplier::get);
    }

    @SafeVarargs
    public static FourToOneAmuletType of(Supplier<ItemStack> amulet, Supplier<? extends AmuletType>... types) {
        return new FourToOneAmuletType(List.of(types), amulet);
    }

    @Override
    public boolean canObtain(ServerPlayer player, DamageSource source) {
        return false;
    }

    @Override
    public boolean matches(ItemLike item) {
        for (AmuletType type : this.types) {
            if (type.matches(item)) return true;
        }
        return super.matches(item);
    }

    @Override
    public void inventoryTick(ServerPlayer player, ItemStack amulet, boolean isEnabled) {
        for (AmuletType type : this.types) {
            // 在这里要考虑到四合一护符未装备，但某个子类型护符已装备的可能性
            type.inventoryTick(player, amulet, isEnabled
                || AmuletManager.INSTANCE.getAmuletsFromInventory(player).entries().stream()
                    .anyMatch(entry -> entry.getKey().value().equals(type)));
        }
    }

    @Override
    public boolean shouldImmuneDamage(ServerPlayer player, DamageSource source) {
        for (AmuletType type : this.types) {
            if (type.shouldImmuneDamage(player, source)) return true;
        }
        return false;
    }
}
