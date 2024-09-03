package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.EntityEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.SpectralAnvilBlock;
import dev.dubhe.anvilcraft.client.renderer.entity.AscendingBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.SpectralBlockRenderer;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.entity.FallingSpectralBlockEntity;
import dev.dubhe.anvilcraft.entity.FloatingBlockEntity;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;

public class ModEntities {
    public static final EntityEntry<? extends AnimateAscendingBlockEntity> ASCENDING_BLOCK_ENTITY = AnvilCraft
        .REGISTRATE
        .entity("animate_ascending_block", AnimateAscendingBlockEntity::new, MobCategory.MISC)
        .renderer(() -> AscendingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends FallingGiantAnvilEntity> FALLING_GIANT_ANVIL = AnvilCraft
        .REGISTRATE
        .entity("falling_giant_anvil", FallingGiantAnvilEntity::new, MobCategory.MISC)
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends FallingSpectralBlockEntity> FALLING_SPECTRAL_BLOCK = AnvilCraft
            .REGISTRATE
            .entity("falling_spectral_block", FallingSpectralBlockEntity::new, MobCategory.MISC)
            .renderer(() -> SpectralBlockRenderer::new)
            .register();

    public static final EntityEntry<? extends FloatingBlockEntity> FLOATING_BLOCK = AnvilCraft
        .REGISTRATE
        .entity("floating_block", FloatingBlockEntity::new, MobCategory.MISC)
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static void register() {
        // intentionally empty
    }
}
