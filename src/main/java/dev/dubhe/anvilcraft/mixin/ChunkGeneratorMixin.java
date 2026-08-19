package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.worldgen.VanillaOverworldBiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Function;

/** Keeps the overworld-like generator independent from data-driven mod world generation. */
@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {
    @ModifyVariable(
        method = "<init>(Lnet/minecraft/world/level/biome/BiomeSource;Ljava/util/function/Function;)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 2
    )
    private static Function<Holder<Biome>, BiomeGenerationSettings> anvilcraft$useOriginalBiomeSettings(
        Function<Holder<Biome>, BiomeGenerationSettings> settingsGetter, BiomeSource biomeSource
    ) {
        return biomeSource instanceof VanillaOverworldBiomeSource
            ? VanillaOverworldBiomeSource::originalGenerationSettings : settingsGetter;
    }

    @ModifyArg(
        method = "createState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;createForNormal("
                + "Lnet/minecraft/world/level/levelgen/RandomState;J"
                + "Lnet/minecraft/world/level/biome/BiomeSource;"
                + "Lnet/minecraft/core/HolderLookup;"
                + ")Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;"
        ),
        index = 3
    )
    private HolderLookup<StructureSet> anvilcraft$keepOnlyVanillaStructureSets(
        HolderLookup<StructureSet> structureSets
    ) {
        ChunkGenerator generator = (ChunkGenerator) (Object) this;
        return generator.getBiomeSource() instanceof VanillaOverworldBiomeSource
            ? new VanillaStructureSetLookup(structureSets) : structureSets;
    }

    private static final class VanillaStructureSetLookup implements HolderLookup<StructureSet> {
        private final HolderLookup<StructureSet> parent;

        private VanillaStructureSetLookup(HolderLookup<StructureSet> parent) {
            this.parent = parent;
        }

        @Override
        public java.util.Optional<Holder.Reference<StructureSet>> get(ResourceKey<StructureSet> key) {
            return parent.get(key).filter(VanillaStructureSetLookup::isVanillaOnly);
        }

        @Override
        public java.util.Optional<net.minecraft.core.HolderSet.Named<StructureSet>> get(
            net.minecraft.tags.TagKey<StructureSet> key
        ) {
            return parent.get(key);
        }

        @Override
        public java.util.stream.Stream<Holder.Reference<StructureSet>> listElements() {
            return parent.listElements().filter(VanillaStructureSetLookup::isVanillaOnly);
        }

        @Override
        public java.util.stream.Stream<net.minecraft.core.HolderSet.Named<StructureSet>> listTags() {
            return parent.listTags();
        }

        private static boolean isVanillaOnly(Holder.Reference<StructureSet> structureSet) {
            return structureSet.unwrapKey()
                .map(key -> "minecraft".equals(key.location().getNamespace()))
                .orElse(false)
                && structureSet.value().structures().stream().allMatch(entry -> entry.structure().unwrapKey()
                    .map(key -> "minecraft".equals(key.location().getNamespace()))
                    .orElse(false));
        }
    }
}
