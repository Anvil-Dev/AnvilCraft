package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** A megastructure definition resolved for one celestial body. */
public record CelestialRefactorOption(
    Megastructure definition,
    Megastructure.Context context,
    int ring,
    Identifier modelLocation,
    String displayName,
    ItemStack material,
    int materialCount
) {
    public static CelestialRefactorOption resolve(Megastructure definition, Megastructure.Context context) {
        int ring = definition.ring(context);
        return new CelestialRefactorOption(
            definition,
            context,
            ring,
            definition.modelLocation(ring),
            definition.displayName(),
            definition.material(),
            definition.materialCount()
        );
    }

    public Identifier id() {
        return this.definition.id();
    }

    public String megastructure() {
        return this.definition.name();
    }

    public boolean auxiliary() {
        return this.definition.auxiliary();
    }

    public float rotation(float baseRotation, float bodyRotation) {
        return this.definition.rotation(this.context, this.ring, baseRotation, bodyRotation);
    }

    public boolean needsMaterial() {
        return this.materialCount > 0 && !this.material.isEmpty();
    }
}
