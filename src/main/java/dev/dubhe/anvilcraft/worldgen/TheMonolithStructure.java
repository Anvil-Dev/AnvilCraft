package dev.dubhe.anvilcraft.worldgen;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.ModStructureTypes;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * 石碑结构：仅在注册表中存在，不提供世界生成入口；
 * 碑体由代码在玩家首次登月时以结构模板放置。
 */
public class TheMonolithStructure extends Structure {
    public static final MapCodec<TheMonolithStructure> CODEC = simpleCodec(TheMonolithStructure::new);

    public TheMonolithStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.THE_MONOLITH.get();
    }
}
