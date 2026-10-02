package dev.dubhe.anvilcraft.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public final class VanillaStructureSetLookup implements HolderLookup<StructureSet> {
    private final HolderLookup<StructureSet> parent;

    public VanillaStructureSetLookup(HolderLookup<StructureSet> parent) {
        this.parent = parent;
    }

    @Override
    public java.util.Optional<Holder.Reference<StructureSet>> get(ResourceKey<StructureSet> key) {
        return this.parent.get(key).filter(VanillaStructureSetLookup::isVanillaOnly);
    }

    @Override
    public java.util.Optional<net.minecraft.core.HolderSet.Named<StructureSet>> get(
        net.minecraft.tags.TagKey<StructureSet> key
    ) {
        return this.parent.get(key);
    }

    @Override
    public java.util.stream.Stream<Holder.Reference<StructureSet>> listElements() {
        return this.parent.listElements().filter(VanillaStructureSetLookup::isVanillaOnly);
    }

    @Override
    public java.util.stream.Stream<net.minecraft.core.HolderSet.Named<StructureSet>> listTags() {
        return this.parent.listTags();
    }

    private static boolean isVanillaOnly(Holder.Reference<StructureSet> structureSet) {
        return structureSet.unwrapKey()
            .map(key -> "minecraft".equals(key.identifier().getNamespace()))
            .orElse(false)
            && structureSet.value().structures().stream().allMatch(entry -> entry.structure().unwrapKey()
                .map(key -> "minecraft".equals(key.identifier().getNamespace()))
                .orElse(false));
    }
}
