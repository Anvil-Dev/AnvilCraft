package dev.dubhe.anvilcraft.api.item.property;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.CollectionUtil;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

/**
 * 多相
 *
 * @param phases 所有相
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record Multiphase(LinkedList<Phase> phases) {
    public static final Multiphase EMPTY = make(Component.literal("Empty"));

    private static final String DEFAULT_SUFFIXES = "αβγδεζηθικλμνξοπρστυφχψω";
    private static final int MAX_PHASE_COUNT = 4;

    public static Component makeName(int index) {
        //noinspection DataFlowIssue
        index = index % Math.min(DEFAULT_SUFFIXES.length(), MAX_PHASE_COUNT);
        return Component.translatableWithFallback(
            "tooltip.anvilcraft.property.multiphase.name." + index,
            "" + DEFAULT_SUFFIXES.charAt(index));
    }

    public static Component makeSuffix(int index) {
        //noinspection DataFlowIssue
        index = index % Math.min(DEFAULT_SUFFIXES.length(), MAX_PHASE_COUNT);
        return Component.translatableWithFallback(
            "tooltip.anvilcraft.property.multiphase.suffix." + index,
            "-" + DEFAULT_SUFFIXES.charAt(index));
    }

    public static final Codec<Multiphase> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        CodecUtil.linkedListOf(Phase.CODEC).fieldOf("phases").forGetter(Multiphase::phases)
    ).apply(inst, Multiphase::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Multiphase> STREAM_CODEC = StreamCodec.composite(
        Phase.STREAM_CODEC.apply(ByteBufCodecs.collection(CollectionUtil::newLinkedList)), Multiphase::phases,
        Multiphase::new
    );

    public Phase peekFirst() {
        return Objects.requireNonNull(this.phases.peek());
    }

    /**
     * 构建一个全新的多相<br>
     * 该方法用于工具初始化
     *
     * @param name 原始名称，不含后缀
     * @return 一个全新的多相
     */
    public static Multiphase make(Component name) {
        return make(name, 2);
    }

    /**
     * 构建一个全新的多相<br>
     * 该方法用于工具初始化
     *
     * @param name 原始名称，不含后缀
     * @return 一个全新的多相
     */
    @SuppressWarnings("SameParameterValue")
    private static Multiphase make(Component name, int phaseCount) {
        LinkedList<Phase> phases = new LinkedList<>();
        for (int i = 0; i < phaseCount; i++) {
            phases.add(Phase.create(i).withItemName(name.copy().append(makeSuffix(i))));
        }
        return new Multiphase(phases);
    }

    /**
     * 构建一个全新的多相<br>
     *
     * @param name         原始名称，不含后缀
     * @param enchantments 初始附魔，用于α相
     * @return 一个全新的多相
     */
    public static Multiphase make(Component name, @Nullable ItemEnchantments enchantments) {
        return make(name, enchantments, 2);
    }

    /**
     * 构建一个全新的多相<br>
     *
     * @param name         原始名称，不含后缀
     * @param enchantments 初始附魔，用于α相
     * @return 一个全新的多相
     */
    @SuppressWarnings("SameParameterValue")
    private static Multiphase make(Component name, @Nullable ItemEnchantments enchantments, int phaseCount) {
        LinkedList<Phase> phases = new LinkedList<>();
        for (int i = 0; i < phaseCount; i++) {
            Phase phase = Phase.create(i).withItemName(name.copy().append(makeSuffix(i)));
            if (i == 0) {
                phase = phase.withEnchantments(enchantments == null ? ItemEnchantments.EMPTY : enchantments);
            }
            phases.add(phase);
        }
        return new Multiphase(phases);
    }

    /**
     * 使用输入的数据构建一个全新的多相，并传入物品
     *
     * @param original 原始物品
     * @param dataS    数据组，只取前两个非null数据作α相和β相
     * @return 一个全新的多相
     */
    public static Multiphase make(Item original, PhaseData... dataS) {
        if (dataS.length == 0) throw new IllegalArgumentException("Unexpect length 0 phase data");

        LinkedList<Phase> phases = new LinkedList<>();
        for (int i = 0; i < dataS.length; i++) {
            PhaseData data = dataS[i];
            Phase phase;
            if (data == null) {
                phase = Phase.create(i).withName(makeName(i)).withItemName(original.getDescription().copy().append(makeSuffix(i)));
            } else {
                phase = Phase.create(i)
                    .withRepairCost(data.repairCost())
                    .withEnchantments(data.enchantments())
                    .withStoredEnchantments(data.storedEnchantments());
                if (data.customName() != null && !data.customName().equals(Component.empty())) {
                    phase = phase.withCustomName(data.customName().copy());
                }
                if (data.itemName() != null && !data.itemName().equals(Component.empty())) {
                    phase = phase.withItemName(data.itemName().copy());
                } else {
                    phase = phase.withItemName(original.getDescription().copy().append(makeSuffix(i)));
                }
            }
            phases.add(phase);
        }
        return new Multiphase(phases);
    }

    public void applyToStack(ItemStack stack) {
        Objects.requireNonNull(this.phases.peek(), "Unexpect no phase multiphase").applyToStack(stack);
    }

    public void cyclePhases(ItemStack stack) {
        this.cyclePhases(stack, (byte) 1);
    }

    public void cyclePhases(ItemStack stack, byte index) {
        if (index == 0) return;
        LinkedList<Phase> phases = this.phases;
        for (int i = 0; i < (index - 1) % phases.size(); i++) {
            phases.offer(phases.poll());
        }
        final Phase[] storing = {phases.poll()};

        Optional<Phase> beta = Optional.ofNullable(phases.peek());
        beta.map(phase -> phase.customName.map(presentValue -> stack.set(DataComponents.CUSTOM_NAME, presentValue))
            .orElseGet(() -> {
                Component c = stack.get(DataComponents.CUSTOM_NAME);
                stack.remove(DataComponents.CUSTOM_NAME);
                return c;
            })
        ).ifPresentOrElse(
            name -> storing[0] = storing[0].withCustomName(name),
            () -> storing[0] = storing[0].clearCustomName());
        beta.map(phase -> phase.itemName.map(presentValue -> stack.set(DataComponents.ITEM_NAME, presentValue))
            .orElseGet(() -> {
                Component c = stack.get(DataComponents.ITEM_NAME);
                stack.remove(DataComponents.ITEM_NAME);
                return c;
            })
        ).ifPresentOrElse(
            name -> storing[0] = storing[0].withItemName(name),
            () -> storing[0] = storing[0].clearItemName());

        storing[0] = storing[0]
            .withRepairCost(
                beta.map(phase -> stack.set(DataComponents.REPAIR_COST, phase.repairCost))
                    .map(repairCost -> Math.max(repairCost, 0))
                    .orElse(0))
            .withEnchantments(
                beta.map(phase -> stack.set(DataComponents.ENCHANTMENTS, phase.enchantments))
                    .orElse(ItemEnchantments.EMPTY))
            .withStoredEnchantments(
                beta.map(phase -> stack.set(DataComponents.STORED_ENCHANTMENTS, phase.storedEnchantments))
                    .orElse(ItemEnchantments.EMPTY));

        phases.offer(storing[0]);
        stack.set(ModComponents.MULTIPHASE, new Multiphase(phases));
    }

    public Multiphase copy() {
        LinkedList<Phase> phases = new LinkedList<>();
        for (Phase phase : this.phases) {
            phases.offer(phase.copy());
        }
        return new Multiphase(phases);
    }

    public Multiphase withAlpha(Phase alpha) {
        @SuppressWarnings("unchecked")
        LinkedList<Phase> phases = (LinkedList<Phase>) this.phases.clone();
        phases.pollFirst();
        phases.offerFirst(alpha);
        return new Multiphase(phases);
    }

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }

    public record Phase(
        int index, Component phaseName,
        Optional<Component> customName, Optional<Component> itemName,
        int repairCost, @NotNull ItemEnchantments enchantments, @NotNull ItemEnchantments storedEnchantments
    ) {
        // TODO: for compatibility, remove in future
        private static Phase compat(
            int index, Component phaseName,
            Optional<Component> customName, Optional<Component> itemName,
            int repairCost, @NotNull ItemEnchantments enchantments
        ) {
            return new Phase(
                index, phaseName, customName, itemName, repairCost, enchantments, ItemEnchantments.EMPTY);
        }
        
        // TODO: for compatibility, rename to CODEC in future
        public static final Codec<Phase> TRUE_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("index").forGetter(Phase::index),
            ComponentSerialization.FLAT_CODEC.fieldOf("phaseName").forGetter(Phase::phaseName),
            ComponentSerialization.FLAT_CODEC.optionalFieldOf("customName").forGetter(Phase::customName),
            ComponentSerialization.FLAT_CODEC.optionalFieldOf("itemName").forGetter(Phase::itemName),
            Codec.INT.fieldOf("repairCost").forGetter(Phase::repairCost),
            ItemEnchantments.CODEC.fieldOf("enchantments").forGetter(Phase::enchantments),
            ItemEnchantments.CODEC.fieldOf("storedEnchantments").forGetter(Phase::storedEnchantments)
        ).apply(inst, Phase::new));
        // TODO: for compatibility, remove in future
        public static final Codec<Phase> COMPATIBILITY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("index").forGetter(Phase::index),
            ComponentSerialization.FLAT_CODEC.fieldOf("phaseName").forGetter(Phase::phaseName),
            ComponentSerialization.FLAT_CODEC.optionalFieldOf("customName").forGetter(Phase::customName),
            ComponentSerialization.FLAT_CODEC.optionalFieldOf("itemName").forGetter(Phase::itemName),
            Codec.INT.fieldOf("repairCost").forGetter(Phase::repairCost),
            ItemEnchantments.CODEC.fieldOf("enchantments").forGetter(Phase::enchantments)
        ).apply(inst, Phase::compat));
        public static final Codec<Phase> CODEC = Codec.either(TRUE_CODEC, COMPATIBILITY_CODEC)
            .xmap(Either::unwrap, Either::left);
        public static final StreamCodec<RegistryFriendlyByteBuf, Phase> STREAM_CODEC = CodecUtil.composite(
            ByteBufCodecs.INT, Phase::index,
            ComponentSerialization.STREAM_CODEC, Phase::phaseName,
            ComponentSerialization.OPTIONAL_STREAM_CODEC, Phase::customName,
            ComponentSerialization.OPTIONAL_STREAM_CODEC, Phase::itemName,
            ByteBufCodecs.INT, Phase::repairCost,
            ItemEnchantments.STREAM_CODEC, Phase::enchantments,
            ItemEnchantments.STREAM_CODEC, Phase::storedEnchantments,
            Phase::new
        );

        public Phase(int index, Component name, @Nullable ItemEnchantments enchantments, @Nullable ItemEnchantments storedEnchantments) {
            this(index, name, 0, enchantments, storedEnchantments);
        }

        public Phase(
            int index, Component name, int repairCost,
            @Nullable ItemEnchantments enchantments, @Nullable ItemEnchantments storedEnchantments
        ) {
            this(
                index, name, Optional.empty(), Optional.empty(), repairCost,
                enchantments == null ? ItemEnchantments.EMPTY : enchantments,
                storedEnchantments == null ? ItemEnchantments.EMPTY : storedEnchantments
            );
        }

        public Phase(
            int index, Component name, @Nullable Component customName, @Nullable Component itemName, int repairCost,
            @NotNull ItemEnchantments enchantments, @NotNull ItemEnchantments storedEnchantments
        ) {
            this(
                index,
                name,
                Objects.equals(customName, Component.empty()) ? Optional.empty() : Optional.ofNullable(customName),
                Objects.equals(itemName, Component.empty()) ? Optional.empty() : Optional.ofNullable(itemName),
                repairCost, enchantments, storedEnchantments
            );
        }

        public @Nullable Component getCustomName() {
            return this.customName.map(Component::copy).orElse(null);
        }

        public @Nullable Component getItemName() {
            return this.itemName.map(Component::copy).orElse(null);
        }

        public static Phase create(Multiphase multiphase) {
            return create(multiphase.phases.size());
        }

        public static Phase create(int index) {
            return new Phase(
                index, Multiphase.makeName(index), Optional.empty(), Optional.empty(), 0,
                ItemEnchantments.EMPTY, ItemEnchantments.EMPTY);
        }

        public static Phase make(int index, Component name, @Nullable ItemEnchantments enchantments) {
            return create(index)
                .withCustomName(name)
                .withEnchantments(enchantments == null ? ItemEnchantments.EMPTY : enchantments);
        }

        public Phase withName(Component phaseName) {
            return new Phase(
                this.index, phaseName, this.customName, this.itemName, this.repairCost,
                this.enchantments, this.storedEnchantments);
        }

        public Phase withCustomName(@Nullable Component customName) {
            return new Phase(
                this.index, this.phaseName, Optional.ofNullable(customName), this.itemName, this.repairCost,
                this.enchantments, this.storedEnchantments);
        }

        public Phase withItemName(@Nullable Component itemName) {
            return new Phase(
                this.index, this.phaseName, this.customName, Optional.ofNullable(itemName), this.repairCost,
                this.enchantments, this.storedEnchantments);
        }

        public Phase clearCustomName() {
            return new Phase(
                this.index, this.phaseName, Optional.empty(), this.itemName, this.repairCost,
                this.enchantments, this.storedEnchantments);
        }

        public Phase clearItemName() {
            return new Phase(
                this.index, this.phaseName, this.customName, Optional.empty(), this.repairCost,
                this.enchantments, this.storedEnchantments);
        }

        public Phase withRepairCost(int repairCost) {
            return new Phase(
                this.index, this.phaseName, this.customName, this.itemName, repairCost,
                this.enchantments, this.storedEnchantments);
        }

        public Phase withEnchantments(ItemEnchantments enchantments) {
            return new Phase(
                this.index, this.phaseName, this.customName, this.itemName, this.repairCost,
                enchantments, this.storedEnchantments);
        }

        public Phase withStoredEnchantments(ItemEnchantments storedEnchantments) {
            return new Phase(
                this.index, this.phaseName, this.customName, this.itemName, this.repairCost,
                this.enchantments, storedEnchantments);
        }

        public Phase addRepairCost(int repairCost) {
            return new Phase(
                this.index, this.phaseName, this.customName, this.itemName, this.repairCost + repairCost,
                this.enchantments, this.storedEnchantments);
        }

        public Phase addEnchantments(ItemEnchantments enchantments) {
            ItemEnchantments original = this.enchantments;
            ItemEnchantments.Mutable originalMut = new ItemEnchantments.Mutable(original);
            for (Holder<Enchantment> enchantmentHolder : enchantments.keySet()) {
                if (original.keySet().contains(enchantmentHolder)) {
                    int originalLevel = original.getLevel(enchantmentHolder);
                    int newLevel = enchantments.getLevel(enchantmentHolder);
                    originalMut.set(enchantmentHolder, Math.max(originalLevel, newLevel));
                } else {
                    originalMut.set(enchantmentHolder, enchantments.getLevel(enchantmentHolder));
                }
            }
            return new Phase(
                this.index, this.phaseName, this.customName, this.itemName, this.repairCost,
                originalMut.toImmutable(), this.storedEnchantments);
        }

        public Phase addStoredEnchantments(ItemEnchantments storedEnchantments) {
            ItemEnchantments original = this.storedEnchantments;
            ItemEnchantments.Mutable originalMut = new ItemEnchantments.Mutable(original);
            for (Holder<Enchantment> enchantmentHolder : storedEnchantments.keySet()) {
                if (original.keySet().contains(enchantmentHolder)) {
                    int originalLevel = original.getLevel(enchantmentHolder);
                    int newLevel = storedEnchantments.getLevel(enchantmentHolder);
                    originalMut.set(enchantmentHolder, Math.max(originalLevel, newLevel));
                } else {
                    originalMut.set(enchantmentHolder, storedEnchantments.getLevel(enchantmentHolder));
                }
            }
            return new Phase(
                this.index, this.phaseName, this.customName, this.itemName, this.repairCost,
                this.enchantments, originalMut.toImmutable());
        }

        public void applyToStack(ItemStack stack) {
            if (this.customName.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_NAME);
            } else {
                stack.set(DataComponents.CUSTOM_NAME, this.customName.get());
            }
            if (this.itemName.isEmpty()) {
                stack.remove(DataComponents.ITEM_NAME);
            } else {
                stack.set(DataComponents.ITEM_NAME, this.itemName.get());
            }
            stack.set(DataComponents.REPAIR_COST, this.repairCost());
            stack.set(DataComponents.ENCHANTMENTS, this.enchantments());
            stack.set(DataComponents.STORED_ENCHANTMENTS, this.storedEnchantments());
        }

        public Phase copy() {
            return new Phase(
                this.index,
                this.phaseName.copy(),
                this.customName.map(Component::copy),
                this.itemName.map(Component::copy),
                this.repairCost,
                new ItemEnchantments.Mutable(this.enchantments).toImmutable(),
                new ItemEnchantments.Mutable(this.storedEnchantments).toImmutable());
        }

        @Override
        public String toString() {
            return "Phase{custom_name: %s, item_name: %s, repair_cost: %s, enchantments: %s, stored_enchantments: %s}"
                .formatted(this.customName, this.itemName, this.repairCost, this.enchantments, this.storedEnchantments);
        }
    }

    public record PhaseData(
        @Nullable Component customName, @Nullable Component itemName,
        int repairCost, @Nullable ItemEnchantments enchantments, @Nullable ItemEnchantments storedEnchantments
    ) {
        public static PhaseData of(
            @Nullable Component customName, @Nullable Component itemName,
            int repairCost, @Nullable ItemEnchantments enchantments, @Nullable ItemEnchantments storedEnchantments
        ) {
            return new PhaseData(customName, itemName, repairCost, enchantments, storedEnchantments);
        }

        @Override
        public ItemEnchantments enchantments() {
            return this.enchantments == null ? ItemEnchantments.EMPTY : this.enchantments;
        }

        @Override
        public ItemEnchantments storedEnchantments() {
            return storedEnchantments == null ? ItemEnchantments.EMPTY : this.storedEnchantments;
        }
    }
}
