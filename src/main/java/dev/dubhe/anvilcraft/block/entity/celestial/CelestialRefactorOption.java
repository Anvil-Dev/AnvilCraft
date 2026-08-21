package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.megastructure.BaseMegastructureHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;

/** A megastructure definition resolved for one celestial body. */
public record CelestialRefactorOption(
    Megastructure definition,
    @Nullable Megastructure.Context context,
    int ring,
    ResourceLocation modelLocation,
    String displayName,
    ItemStack material,
    int materialCount
) {

    /** Resolve a registered definition for the supplied CFA context. */
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

    /** Compatibility constructor for add-ons written against the pre-registry API. */
    public CelestialRefactorOption(
        int ring,
        String megastructure,
        ResourceLocation modelLocation,
        String displayName,
        ItemStack material,
        int materialCount
    ) {
        this(
            legacyDefinition(ring, megastructure, modelLocation, displayName),
            null,
            ring,
            modelLocation,
            displayName,
            material,
            materialCount
        );
    }

    public ResourceLocation id() {
        return this.definition.id();
    }

    /** Legacy name accessor retained for handlers, renderers and add-ons. */
    public String megastructure() {
        return this.definition.name();
    }

    public boolean auxiliary() {
        return this.definition.auxiliary();
    }

    public float rotation(float baseRotation, float bodyRotation) {
        if (this.context == null) return baseRotation;
        return this.definition.rotation(this.context, this.ring, baseRotation, bodyRotation);
    }

    public boolean needsMaterial() {
        return this.materialCount > 0 && !this.material.isEmpty();
    }

    /**
     * Creates an option in the old shape. New code should register a
     * {@link Megastructure} instead, but keeping these factories avoids an
     * unnecessary source break for add-ons targeting 1.21.
     */
    @Deprecated
    public static CelestialRefactorOption noMaterial(
        int ring, String megastructure, ResourceLocation modelLocation, String displayName
    ) {
        return new CelestialRefactorOption(
            ring,
            megastructure,
            modelLocation,
            displayName,
            ItemStack.EMPTY,
            0
        );
    }

    @Deprecated
    public static CelestialRefactorOption withMaterial(
        int ring,
        String megastructure,
        ResourceLocation modelLocation,
        String displayName,
        ItemLike material,
        int materialCount
    ) {
        return new CelestialRefactorOption(
            ring,
            megastructure,
            modelLocation,
            displayName,
            new ItemStack(material),
            materialCount
        );
    }

    private static Megastructure legacyDefinition(
        int ring,
        String name,
        ResourceLocation modelLocation,
        String displayName
    ) {
        Megastructure.Builder builder = Megastructure.builder(AnvilCraft.of(name), name)
            .displayName(displayName)
            .ring(ring)
            .model(ring, modelLocation)
            .handler(() -> new LegacyHandler(name));
        if ("stellar_evolution_accelerator".equals(name)) builder.auxiliary();
        return builder.build();
    }

    private static final class LegacyHandler extends BaseMegastructureHandler {
        private final String name;

        private LegacyHandler(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public void serverTick(CelestialForgingAnvilBlockEntity be) {
        }
    }
}
