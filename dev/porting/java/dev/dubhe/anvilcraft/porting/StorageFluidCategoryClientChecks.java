package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.init.storage.ModCategories;
import dev.dubhe.anvilcraft.saved.storage.category.FluidCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;

public final class StorageFluidCategoryClientChecks {
    public static void run(Minecraft client) {
        var category = client.level.registryAccess().lookupOrThrow(ModRegistryKeys.CATEGORY).getOrThrow(ModCategories.FLUID).value();
        var icon = category.icon().create();
        if (!(category instanceof FluidCategory) || !icon.is(Items.WATER_BUCKET)) {
            throw new IllegalStateException("客户端流体分类或图标未正确同步");
        }
        var state = new ItemStackRenderState();
        client.getItemModelResolver().updateForTopItem(state, icon, ItemDisplayContext.GUI, client.level, client.player, 0);
        var material = state.pickParticleMaterial(RandomSource.create(0));
        if (material == null || material.sprite().contents().name().getPath().equals("missingno")) {
            throw new IllegalStateException("流体分类图标未正确加载");
        }
        AnvilCraft.LOGGER.info("PORT_FLUID_CATEGORY_CLIENT_PASSED");
    }
}
