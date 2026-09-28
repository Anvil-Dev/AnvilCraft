package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class RuinsSodiumGeometryProbe {
    public static void verify(RuinsBlockEntity ruins, boolean global) {
        try {
            var renderer = SodiumWorldRenderer.instance();
            var field = SodiumWorldRenderer.class.getDeclaredField("renderSectionManager");
            field.setAccessible(true);
            var manager = (RenderSectionManager) field.get(renderer);
            var method = RenderSectionManager.class.getDeclaredMethod("getRenderSection", int.class, int.class, int.class);
            method.setAccessible(true);
            var pos = ruins.getBlockPos();
            var section = (RenderSection) method.invoke(manager, pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            var region = section.getRegion();
            int globals = count(region.getGlobalBlockEntities(section.getSectionIndex()), ruins);
            int culled = count(region.getCulledBlockEntities(section.getSectionIndex()), ruins);
            if (globals != (global ? 1 : 0) || culled != (global ? 0 : 1)) {
                throw new IllegalStateException("Sodium ruins routing at " + pos + ": global=" + globals + ", section=" + culled);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int count(BlockEntity @Nullable [] entries, BlockEntity target) {
        if (entries == null) return 0;
        int count = 0;
        for (BlockEntity entry : entries) {
            if (entry == target) count++;
        }
        return count;
    }
}
