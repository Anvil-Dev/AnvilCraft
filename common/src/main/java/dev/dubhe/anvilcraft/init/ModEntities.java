package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.EntityEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.entity.AscendingBlockEntity;
import dev.dubhe.anvilcraft.entity.render.AscendingBlockRenderer;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityEntry<? extends AscendingBlockEntity> ASCENDING_BLOCK_ENTITY = AnvilCraft.REGISTRATE
            .entity("ascending_block", AscendingBlockEntity::new, MobCategory.MISC)
            .renderer(() -> AscendingBlockRenderer::new)
            .register();

    public static void register() {
        // intentionally empty
    }
}
