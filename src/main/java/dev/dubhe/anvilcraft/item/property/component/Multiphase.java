package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.util.EnchantmentUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Multiphase(List<Phase> phases, int activePhase, boolean merciless) {
    public static final int MIN_PHASE_COUNT = 2;
    public static final int MAX_PHASE_COUNT = 4;
    private static final List<String> DEFAULT_NAMES = List.of("α", "β", "γ", "δ");

    private static final MapCodec<Multiphase> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Phase.CODEC.codec().listOf().fieldOf("phases").forGetter(Multiphase::phases),
        Codec.INT.fieldOf("active_phase").forGetter(Multiphase::activePhase),
        Codec.BOOL.fieldOf("merciless").forGetter(Multiphase::merciless)
    ).apply(instance, Multiphase::new));
    private static final MapCodec<Multiphase> LEGACY_CODEC = Codec.PASSTHROUGH.fieldOf("id").xmap(
        ignored -> create(),
        ignored -> new Dynamic<>(JsonOps.INSTANCE, JsonOps.INSTANCE.emptyMap())
    );
    public static final MapCodec<Multiphase> CODEC = Codec.mapEither(DATA_CODEC, LEGACY_CODEC).xmap(
        either -> either.map(multiphase -> multiphase, multiphase -> multiphase),
        Either::left
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Multiphase> STREAM_CODEC = StreamCodec.composite(
        Phase.STREAM_CODEC.apply(ByteBufCodecs.list()),
        Multiphase::phases,
        ByteBufCodecs.VAR_INT,
        Multiphase::activePhase,
        ByteBufCodecs.BOOL,
        Multiphase::merciless,
        Multiphase::new
    );

    public Multiphase {
        phases = List.copyOf(phases);
        if (phases.size() < MIN_PHASE_COUNT || phases.size() > MAX_PHASE_COUNT) {
            throw new IllegalArgumentException("Multiphase phase count must be between 2 and 4");
        }
        if (activePhase < 0 || activePhase >= phases.size()) {
            throw new IllegalArgumentException("Active phase is out of bounds: " + activePhase);
        }
    }

    public static Multiphase create() {
        return new Multiphase(List.of(Phase.EMPTY, Phase.EMPTY), 0, false);
    }

    public static Component makeName(int index) {
        return Component.translatableWithFallback(
            "tooltip.anvilcraft.property.multiphase.name." + index,
            DEFAULT_NAMES.get(index)
        );
    }

    public static Component makeSuffix(int index) {
        return Component.translatableWithFallback(
            "tooltip.anvilcraft.property.multiphase.suffix." + index,
            "-" + DEFAULT_NAMES.get(index)
        );
    }

    public static Component firstPhaseName(Component name) {
        return name.copy().append(makeSuffix(0));
    }

    public Component phaseDisplayName(int index, boolean withMerciless) {
        Component name = this.phases.get(index).customName().isPresent()
            ? this.phases.get(index).customName().get().copy()
            : makeName(index);
        if (withMerciless) {
            return name.copy().append(Component.translatable("screen.anvilcraft.multiphase.merciless"));
        }
        return name;
    }

    public Multiphase capture(ItemStack stack) {
        ItemEnchantments enchantments;
        if (this.merciless) {
            Merciless.absorbEnchantments(stack);
            enchantments = stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY);
        } else {
            enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        List<Phase> captured = new ArrayList<>(this.phases);
        captured.set(this.activePhase, Phase.capture(stack, enchantments));
        return new Multiphase(captured, this.activePhase, this.merciless);
    }

    public Multiphase forDisplay(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (this.merciless) {
            enchantments = EnchantmentUtil.merge(
                enchantments,
                stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY)
            );
        }
        List<Phase> displayed = new ArrayList<>(this.phases);
        displayed.set(this.activePhase, Phase.capture(stack, enchantments));
        return new Multiphase(displayed, this.activePhase, this.merciless);
    }

    public void select(ItemStack stack, int phaseIndex, boolean enableMerciless) {
        if (phaseIndex < 0 || phaseIndex >= this.phases.size()) return;
        Multiphase selected = this.capture(stack).withSelection(phaseIndex, enableMerciless);
        selected.applyToStack(stack);
        stack.set(ModComponents.MULTIPHASE, selected);
    }

    public void cycle(ItemStack stack) {
        this.select(stack, (this.activePhase + 1) % this.phases.size(), this.merciless);
    }

    public void initialize(ItemStack stack) {
        this.applyToStack(stack);
        stack.set(ModComponents.MULTIPHASE, this);
    }

    public boolean addPhase(ItemStack stack) {
        Multiphase captured = this.capture(stack);
        if (captured.phases.size() >= MAX_PHASE_COUNT) return false;
        List<Phase> expanded = new ArrayList<>(captured.phases);
        expanded.add(Phase.EMPTY);
        stack.set(ModComponents.MULTIPHASE, new Multiphase(expanded, captured.activePhase, captured.merciless));
        return true;
    }

    public void applySelectionPreview(ItemStack stack, int phaseIndex, boolean enableMerciless) {
        if (phaseIndex < 0 || phaseIndex >= this.phases.size()) return;
        this.withSelection(phaseIndex, enableMerciless).applyToStack(stack);
    }

    private Multiphase withSelection(int phaseIndex, boolean enableMerciless) {
        return new Multiphase(this.phases, phaseIndex, enableMerciless);
    }

    private void applyToStack(ItemStack stack) {
        Merciless.disable(stack);
        this.phases.get(this.activePhase).applyToStack(stack);
        stack.set(
            DataComponents.ITEM_NAME,
            stack.getItem().getDescription().copy().append(makeSuffix(this.activePhase))
        );
        if (this.merciless) Merciless.enable(stack);
    }

    public record Phase(Optional<Component> customName, int repairCost, ItemEnchantments enchantments) {
        public static final Phase EMPTY = new Phase(Optional.empty(), 0, ItemEnchantments.EMPTY);
        public static final MapCodec<Phase> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ComponentSerialization.FLAT_CODEC.optionalFieldOf("custom_name").forGetter(Phase::customName),
            Codec.INT.fieldOf("repair_cost").forGetter(Phase::repairCost),
            ItemEnchantments.CODEC.fieldOf("enchantments").forGetter(Phase::enchantments)
        ).apply(instance, Phase::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Phase> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.OPTIONAL_STREAM_CODEC,
            Phase::customName,
            ByteBufCodecs.VAR_INT,
            Phase::repairCost,
            ItemEnchantments.STREAM_CODEC,
            Phase::enchantments,
            Phase::new
        );

        public Phase {
            customName = customName.map(Component::copy);
        }

        public static Phase fromInput(ItemStack stack) {
            return capture(
                stack,
                EnchantmentUtil.merge(
                    stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY),
                    stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY)
                )
            );
        }

        private static Phase capture(ItemStack stack, ItemEnchantments enchantments) {
            return new Phase(
                Optional.ofNullable(stack.get(DataComponents.CUSTOM_NAME)),
                stack.getOrDefault(DataComponents.REPAIR_COST, 0),
                enchantments
            );
        }

        private void applyToStack(ItemStack stack) {
            if (this.customName.isPresent()) {
                stack.set(DataComponents.CUSTOM_NAME, this.customName.get().copy());
            } else {
                stack.remove(DataComponents.CUSTOM_NAME);
            }
            stack.set(DataComponents.REPAIR_COST, this.repairCost);
            stack.set(DataComponents.ENCHANTMENTS, this.enchantments);
        }

    }
}
