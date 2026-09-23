package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.fluid.HoneyFluid;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public final class FluidHandlerWrapper {
    private final ResourceHandler<FluidResource> handler;

    public FluidHandlerWrapper(ResourceHandler<FluidResource> handler) {
        this.handler = handler;
    }

    public @Nullable ItemStack fillFromItem(ItemStack container, boolean simulate, @Nullable RandomSource random) {
        if (container.isEmpty()) return null;
        var single = new ItemStacksResourceHandler(1);
        single.set(0, ItemResource.of(container), 1);
        var source = ItemAccess.forHandlerIndexStrict(single, 0).getCapability(Capabilities.Fluid.ITEM);
        if (source != null) {
            for (int slot = 0; slot < source.size(); slot++) {
                FluidResource resource = source.getResource(slot);
                int amount = source.getAmountAsInt(slot);
                if (resource.isEmpty() || amount <= 0) continue;
                try (var transaction = Transaction.openRoot()) {
                    if (source.extract(slot, resource, amount, transaction) != amount
                        || this.handler.insert(resource, amount, transaction) != amount) return null;
                    ItemStack result = single.getResource(0).toStack(single.getAmountAsInt(0));
                    if (!simulate) transaction.commit();
                    return result;
                }
            }
            return null;
        }
        boolean experience = container.is(Items.EXPERIENCE_BOTTLE);
        var potion = container.get(DataComponents.POTION_CONTENTS);
        if (!experience && (potion == null || !potion.is(Potions.WATER))) return null;
        FluidResource resource = FluidResource.of(experience ? ModFluids.EXP_FLUID.get() : Fluids.WATER);
        try (var transaction = Transaction.openRoot()) {
            int amount = FluidType.BUCKET_VOLUME / 4;
            if (this.handler.insert(resource, amount, transaction) != amount) return null;
            if (!simulate && (!experience || random == null || random.nextBoolean())) transaction.commit();
            return Items.GLASS_BOTTLE.getDefaultInstance();
        }
    }

    public @Nullable ItemStack drainToItem(ItemStack container, boolean simulate) {
        boolean bottle = container.is(Items.GLASS_BOTTLE);
        if (!bottle && !container.is(Items.BUCKET)) return null;
        int amount = FluidType.BUCKET_VOLUME / (bottle ? 4 : 1);
        for (int slot = 0; slot < this.handler.size(); slot++) {
            FluidResource resource = this.handler.getResource(slot);
            if (resource.isEmpty() || this.handler.getAmountAsLong(slot) < amount) continue;
            ItemStack result;
            if (!bottle) {
                result = resource.getFluid().getBucket().getDefaultInstance();
            } else if (resource.getFluid() instanceof HoneyFluid) {
                result = Items.HONEY_BOTTLE.getDefaultInstance();
            } else if (resource.is(Fluids.WATER)) {
                result = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            } else if (resource.is(ModFluids.EXP_FLUID.get())) {
                result = Items.EXPERIENCE_BOTTLE.getDefaultInstance();
            } else {
                return null;
            }
            if (result.isEmpty()) return null;
            try (var transaction = Transaction.openRoot()) {
                if (this.handler.extract(slot, resource, amount, transaction) != amount) return null;
                if (!simulate) transaction.commit();
                return result;
            }
        }
        return null;
    }
}
