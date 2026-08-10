package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.block.entity.megastructure.IMegastructureHandler;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Code-registered definition of a Celestial Forging Anvil megastructure.
 */
public final class Megastructure {
    private final Identifier id;
    private final String name;
    private final String displayName;
    private final Prerequisite prerequisite;
    private final ToIntFunction<Context> ringFunction;
    private final RotationFunction rotationFunction;
    private final Map<Integer, Identifier> modelLocations;
    private final @Nullable ItemLike material;
    private final int materialCount;
    private final Supplier<? extends IMegastructureHandler> handlerFactory;
    private final boolean auxiliary;

    private Megastructure(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.prerequisite = builder.prerequisite;
        this.ringFunction = Objects.requireNonNull(builder.ringFunction, "Megastructure ring function was not set");
        this.rotationFunction = builder.rotationFunction;
        this.modelLocations = Collections.unmodifiableMap(new LinkedHashMap<>(builder.modelLocations));
        this.material = builder.material;
        this.materialCount = builder.materialCount;
        this.handlerFactory = Objects.requireNonNull(builder.handlerFactory, "Megastructure handler factory was not set");
        this.auxiliary = builder.auxiliary;
        if (this.modelLocations.isEmpty()) {
            throw new IllegalStateException("Megastructure must provide at least one model: " + this.id);
        }
    }

    public static Builder builder(Identifier id, String name) {
        return new Builder(id, name);
    }

    public Identifier id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String displayName() {
        return this.displayName;
    }

    public Map<Integer, Identifier> modelLocations() {
        return this.modelLocations;
    }

    public ItemStack material() {
        return this.material == null ? ItemStack.EMPTY : new ItemStack(this.material);
    }

    public int materialCount() {
        return this.materialCount;
    }

    public boolean auxiliary() {
        return this.auxiliary;
    }

    public boolean isAvailable(Context context) {
        return this.prerequisite.test(context);
    }

    public int ring(Context context) {
        return this.ringFunction.applyAsInt(context);
    }

    public Identifier modelLocation(int ring) {
        Identifier location = this.modelLocations.get(ring);
        if (location == null) {
            throw new IllegalStateException("No model registered for megastructure " + this.id + " on ring " + ring);
        }
        return location;
    }

    public float rotation(Context context, int ring, float baseRotation, float bodyRotation) {
        return this.rotationFunction.apply(new RotationContext(context.body(), ring, baseRotation, bodyRotation));
    }

    public IMegastructureHandler createHandler() {
        return this.handlerFactory.get();
    }

    public record Context(
        CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
    }

    public record RotationContext(
        CelestialBodyData body,
        int ring,
        float baseRotation,
        float bodyRotation
    ) {
    }

    @FunctionalInterface
    public interface Prerequisite {
        boolean test(Context context);
    }

    @FunctionalInterface
    public interface RotationFunction {
        float apply(RotationContext context);
    }

    public static final class Builder {
        private final Identifier id;
        private final String name;
        private String displayName;
        private Prerequisite prerequisite = context -> true;
        private @Nullable ToIntFunction<Context> ringFunction;
        private RotationFunction rotationFunction = RotationContext::baseRotation;
        private final Map<Integer, Identifier> modelLocations = new LinkedHashMap<>();
        private @Nullable ItemLike material;
        private int materialCount;
        private @Nullable Supplier<? extends IMegastructureHandler> handlerFactory;
        private boolean auxiliary;

        private Builder(Identifier id, String name) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
            this.displayName = "screen.anvilcraft.cfa.megastructure." + name;
        }

        public Builder displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName);
            return this;
        }

        public Builder prerequisite(Prerequisite prerequisite) {
            this.prerequisite = Objects.requireNonNull(prerequisite);
            return this;
        }

        public Builder ring(int ring) {
            return this.ring(context -> ring);
        }

        public Builder ring(ToIntFunction<Context> ringFunction) {
            this.ringFunction = Objects.requireNonNull(ringFunction);
            return this;
        }

        public Builder rotation(RotationFunction rotationFunction) {
            this.rotationFunction = Objects.requireNonNull(rotationFunction);
            return this;
        }

        public Builder model(int ring, Identifier modelLocation) {
            Identifier previous = this.modelLocations.put(ring, Objects.requireNonNull(modelLocation));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate model ring " + ring + " for megastructure " + this.id);
            }
            return this;
        }

        public Builder material(ItemLike material, int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("Megastructure material count must be positive");
            }
            this.material = Objects.requireNonNull(material);
            this.materialCount = count;
            return this;
        }

        public Builder handler(Supplier<? extends IMegastructureHandler> handlerFactory) {
            this.handlerFactory = Objects.requireNonNull(handlerFactory);
            return this;
        }

        public Builder auxiliary() {
            this.auxiliary = true;
            return this;
        }

        public Megastructure build() {
            return new Megastructure(this);
        }
    }
}
