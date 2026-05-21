package dev.dubhe.anvilcraft.saved.multiphase;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.CollectionUtil;
import dev.dubhe.anvilcraft.util.EnchantmentUtil;
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
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

/**
 * 多相
 *
 * @param phases 所有相
 */
public record Multiphase(LinkedList<Phase> phases) {
    public static final Multiphase EMPTY = make(Component.literal("Empty"));

    public static final String DEFAULT_SUFFIXES = "αβγδεζηθικλμνξοπρστυφχψω";
    private static final int MAX_PHASE_COUNT = 4;

    public static Component makeName(int index) {
        index = index % Math.min(DEFAULT_SUFFIXES.length(), MAX_PHASE_COUNT);
        return Component.translatableWithFallback(
            "tooltip.anvilcraft.property.multiphase.name." + index,
            "" + DEFAULT_SUFFIXES.charAt(index)
        );
    }

    public static Component makeSuffix(int index) {
        // noinspection DataFlowIssue
        index = index % Math.min(DEFAULT_SUFFIXES.length(), MAX_PHASE_COUNT);
        return Component.translatableWithFallback(
            "tooltip.anvilcraft.property.multiphase.suffix." + index,
            "-" + DEFAULT_SUFFIXES.charAt(index)
        );
    }

    public static final MapCodec<Multiphase> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        CodecUtil.linkedListOf(Phase.CODEC.codec())
            .fieldOf("phases")
            .forGetter(Multiphase::phases)
    ).apply(inst, Multiphase::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Multiphase> STREAM_CODEC = StreamCodec.composite(
        Phase.STREAM_CODEC.apply(ByteBufCodecs.collection(CollectionUtil::newLinkedList)), Multiphase::phases,
        Multiphase::new
    );

    public Phase peekFirst() {
        return Objects.requireNonNull(this.phases.peek());
    }

    public static Component firstPhaseName(Component name) {
        return name.copy().append(makeSuffix(0));
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
                phase = Phase.create(i).withItemName(original.getDescription().copy().append(makeSuffix(i)));
            } else {
                phase = Phase.create(i)
                    .withRepairCost(data.repairCost())
                    .withEnchantments(data.enchantments());
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

    public void changePhase(ItemStack stack) {
        this.changePhase(stack, (byte) ((this.phases.peek().index + 1) % this.phases.size()));
    }

    public void changePhase(ItemStack stack, byte index) {
        // 若目标相已为首相，返回
        int current = this.phases.peek().index;
        if (index == current) return;

        // 计算使首相为目标相所需的取出放入操作数
        int diff = (index - current + this.phases.size()) % this.phases.size();
        // 取出首相，即需要更新现有值的相
        Phase storing = this.phases.poll();
        // 获取目标相
        Phase beta = this.phases.get(diff - 1);

        // 将目标相的值应用至物品，并存储旧值
        Component customName = stack.set(DataComponents.CUSTOM_NAME, beta.customName().orElse(null));
        if (customName != null) {
            storing = storing.withCustomName(customName);
        } else {
            storing = storing.clearCustomName();
        }

        Component itemName = stack.set(DataComponents.ITEM_NAME, beta.itemName().orElse(null));
        if (itemName != null) {
            storing = storing.withItemName(itemName);
        } else {
            storing = storing.clearItemName();
        }

        int repairCost = Math.max(stack.set(DataComponents.REPAIR_COST, beta.repairCost()), 0);
        storing = storing.withRepairCost(repairCost);

        ItemEnchantments enchantments = stack.set(DataComponents.ENCHANTMENTS, beta.enchantments());
        storing = storing.withEnchantments(Objects.requireNonNullElse(enchantments, ItemEnchantments.EMPTY));

        // 将已更新现有值的首相放回首位
        this.phases.offerFirst(storing);
        // 执行操作，将目标相旋转至首位
        for (int i = 0; i < diff; i++) {
            this.phases.offer(this.phases.poll());
        }
    }

    public Multiphase copy() {
        LinkedList<Phase> phases = new LinkedList<>();
        for (Phase phase : this.phases) {
            phases.offer(phase.copy());
        }
        return new Multiphase(phases);
    }

    public void changeAlpha(Phase alpha) {
        this.phases.pollFirst();
        this.phases.offerFirst(alpha);
    }

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }

    public record Phase(
        int index,
        Component phaseName,
        Optional<Component> customName,
        Optional<Component> itemName,
        int repairCost,
        ItemEnchantments enchantments
    ) {
        public static final MapCodec<Phase> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT
                .fieldOf("index")
                .forGetter(Phase::index),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("phaseName")
                .forGetter(Phase::phaseName),
            ComponentSerialization.FLAT_CODEC
                .optionalFieldOf("customName")
                .forGetter(Phase::customName),
            ComponentSerialization.FLAT_CODEC
                .optionalFieldOf("itemName")
                .forGetter(Phase::itemName),
            Codec.INT
                .fieldOf("repairCost")
                .forGetter(Phase::repairCost),
            ItemEnchantments.CODEC
                .fieldOf("enchantments")
                .forGetter(Phase::enchantments)
        ).apply(inst, Phase::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Phase> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            Phase::index,
            ComponentSerialization.STREAM_CODEC,
            Phase::phaseName,
            ComponentSerialization.OPTIONAL_STREAM_CODEC,
            Phase::customName,
            ComponentSerialization.OPTIONAL_STREAM_CODEC,
            Phase::itemName,
            ByteBufCodecs.INT,
            Phase::repairCost,
            ItemEnchantments.STREAM_CODEC,
            Phase::enchantments,
            Phase::new
        );

        public Phase(int index, Component name, @Nullable ItemEnchantments enchantments) {
            this(index, name, 0, enchantments);
        }

        public Phase(
            int index,
            Component name,
            int repairCost,
            @Nullable ItemEnchantments enchantments
        ) {
            this(
                index,
                name,
                Optional.empty(),
                Optional.empty(),
                repairCost,
                enchantments == null ? ItemEnchantments.EMPTY : enchantments
            );
        }

        public Phase(
            int index,
            Component name,
            @Nullable Component customName,
            @Nullable Component itemName,
            int repairCost,
            ItemEnchantments enchantments
        ) {
            this(
                index,
                name,
                Objects.equals(customName, Component.empty()) ? Optional.empty() : Optional.ofNullable(customName),
                Objects.equals(itemName, Component.empty()) ? Optional.empty() : Optional.ofNullable(itemName),
                repairCost,
                enchantments
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
                index,
                Multiphase.makeName(index),
                Optional.empty(),
                Optional.empty(),
                0,
                ItemEnchantments.EMPTY
            );
        }

        public static Phase make(int index, Component name, @Nullable ItemEnchantments enchantments) {
            return create(index)
                .withCustomName(name)
                .withEnchantments(enchantments == null ? ItemEnchantments.EMPTY : enchantments);
        }

        public Phase merge(Phase phase) {
            return new Phase(
                this.index,
                this.phaseName,
                this.customName.or(phase::customName),
                this.itemName.or(phase::itemName),
                this.repairCost + phase.repairCost,
                EnchantmentUtil.merge(this.enchantments, phase.enchantments)
            );
        }

        public Phase withName(Component phaseName) {
            return new Phase(
                this.index,
                phaseName,
                this.customName,
                this.itemName,
                this.repairCost,
                this.enchantments
            );
        }

        public Phase withCustomName(@Nullable Component customName) {
            return new Phase(
                this.index,
                this.phaseName,
                Optional.ofNullable(customName),
                this.itemName,
                this.repairCost,
                this.enchantments
            );
        }

        public Phase withItemName(@Nullable Component itemName) {
            return new Phase(
                this.index,
                this.phaseName,
                this.customName,
                Optional.ofNullable(itemName),
                this.repairCost,
                this.enchantments
            );
        }

        public Phase clearCustomName() {
            return new Phase(
                this.index,
                this.phaseName,
                Optional.empty(),
                this.itemName,
                this.repairCost,
                this.enchantments
            );
        }

        public Phase clearItemName() {
            return new Phase(
                this.index,
                this.phaseName,
                this.customName,
                Optional.empty(),
                this.repairCost,
                this.enchantments
            );
        }

        public Phase withRepairCost(int repairCost) {
            return new Phase(
                this.index,
                this.phaseName,
                this.customName,
                this.itemName,
                repairCost,
                this.enchantments
            );
        }

        public Phase withEnchantments(ItemEnchantments enchantments) {
            return new Phase(
                this.index,
                this.phaseName,
                this.customName,
                this.itemName,
                this.repairCost,
                enchantments
            );
        }

        public Phase addRepairCost(int repairCost) {
            return new Phase(
                this.index,
                this.phaseName,
                this.customName,
                this.itemName,
                this.repairCost + repairCost,
                this.enchantments
            );
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
                this.index,
                this.phaseName,
                this.customName,
                this.itemName,
                this.repairCost,
                originalMut.toImmutable()
            );
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
        }

        public Phase copy() {
            return new Phase(
                this.index,
                this.phaseName.copy(),
                this.customName.map(Component::copy),
                this.itemName.map(Component::copy),
                this.repairCost,
                new ItemEnchantments.Mutable(this.enchantments).toImmutable()
            );
        }

        @Override
        public String toString() {
            return "Phase{customName: %s, itemName: %s, repairCost: %s, enchantments: %s}"
                .formatted(this.customName, this.itemName, this.repairCost, this.enchantments);
        }
    }

    public record PhaseData(
        @Nullable Component customName,
        @Nullable Component itemName,
        int repairCost,
        @Nullable ItemEnchantments enchantments
    ) {
        public static PhaseData of(
            @Nullable Component customName,
            @Nullable Component itemName,
            int repairCost,
            @Nullable ItemEnchantments enchantments
        ) {
            return new PhaseData(customName, itemName, repairCost, enchantments);
        }

        @Override
        public ItemEnchantments enchantments() {
            return this.enchantments == null ? ItemEnchantments.EMPTY : this.enchantments;
        }
    }
}
