package dev.dubhe.anvilcraft.data.generator.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.dubhe.anvilcraft.init.ModFluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public class FluidTagLoader {
    /**
     * 物品标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateTagsProvider<Fluid> provider) {
        provider.addTag(ModFluidTags.MENGER_SPONGE_CAN_ABSORB).setReplace(false)
            .add(Fluids.WATER)
            .add(Fluids.FLOWING_WATER)
            .add(Fluids.LAVA)
            .add(Fluids.FLOWING_LAVA);
    }
}
