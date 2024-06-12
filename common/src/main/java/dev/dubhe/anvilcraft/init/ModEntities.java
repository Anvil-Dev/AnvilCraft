package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.entity.AscendingBlockRenderer;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;

import net.minecraft.world.entity.MobCategory;

import com.tterrag.registrate.util.entry.EntityEntry;

public class ModEntities {
    public static final EntityEntry<? extends AnimateAscendingBlockEntity> ASCENDING_BLOCK_ENTITY =
            AnvilCraft.REGISTRATE
                    .entity("animate_ascending_block", AnimateAscendingBlockEntity::new, MobCategory.MISC)
                    .renderer(() -> AscendingBlockRenderer::new)
                    .register();

    public static void register() {
        // intentionally empty
    }
}
