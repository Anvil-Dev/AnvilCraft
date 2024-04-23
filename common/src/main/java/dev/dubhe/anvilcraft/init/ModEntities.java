package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.EntityEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.entity.render.AscendingBlockRenderer;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityEntry<? extends AnimateAscendingBlockEntity> ASCENDING_BLOCK_ENTITY = AnvilCraft
        .REGISTRATE
        .entity("animate_ascending_block", AnimateAscendingBlockEntity::new, MobCategory.MISC)
        .renderer(() -> AscendingBlockRenderer::new)
        .register();

    public static void register() {
        // intentionally empty
    }
}
