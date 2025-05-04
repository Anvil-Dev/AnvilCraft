package dev.dubhe.anvilcraft.api.item.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModComponents;
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
import java.util.Optional;

/**
 * 多相
 *
 * @param alpha α相
 * @param beta β相
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record Multiphase(Phase alpha, Phase beta) {
    public static final Multiphase EMPTY = make(Component.literal("Empty"));

    public static final Component ALPHA_NAME_SUFFIX = Component.translatable("tooltip.anvilcraft.property.multiphase.suffix.alpha");
    public static final Component BETA_NAME_SUFFIX = Component.translatable("tooltip.anvilcraft.property.multiphase.suffix.beta");

    public static final Codec<Multiphase> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Phase.CODEC.fieldOf("alpha").forGetter(Multiphase::alpha),
        Phase.CODEC.fieldOf("beta").forGetter(Multiphase::beta)
    ).apply(inst, Multiphase::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Multiphase> STREAM_CODEC =
        StreamCodec.of(Multiphase::encode, Multiphase::decode);

    /**
     * 构建一个全新的多相<br>
     * 该方法目前仅用于多相工具
     *
     * @param name 原始名称，不含后缀
     *
     * @return 一个全新的多相
     */
    public static Multiphase make(Component name) {
        Component newNameAlpha = name.copy().append(ALPHA_NAME_SUFFIX);
        Phase alpha = Phase.EMPTY.withItemName(newNameAlpha);
        Component newNameBeta = name.copy().append(BETA_NAME_SUFFIX);
        Phase beta = Phase.EMPTY.withItemName(newNameBeta);
        return new Multiphase(alpha, beta);
    }

    /**
     * 构建一个全新的多相<br>
     *
     * @param name   原始名称，不含后缀
     * @param enchantments 初始附魔，用于α相
     *
     * @return 一个全新的多相
     */
    public static Multiphase make(Component name, @Nullable ItemEnchantments enchantments) {
        Component newNameAlpha = name.copy().append(ALPHA_NAME_SUFFIX);
        Phase alpha = Phase.EMPTY.withItemName(newNameAlpha).withEnchantments(enchantments == null ? ItemEnchantments.EMPTY : enchantments);
        Component newNameBeta = name.copy().append(BETA_NAME_SUFFIX);
        Phase beta = Phase.EMPTY.withItemName(newNameBeta);
        return new Multiphase(alpha, beta);
    }

    /**
     * 使用输入的数据构建一个全新的多相，并传入物品
     *
     * @param original 原始物品
     * @param dataS 数据组，只取前两个非null数据作α相和β相
     *
     * @return 一个全新的多相
     */
    public static Multiphase make(Item original, PhaseData... dataS) {
        Phase[] phases = new Phase[2];
        for (int i = 0; i < 2; i++) {
            PhaseData data = dataS[i];
            if (data != null) {
                phases[i] = Phase.EMPTY
                    .withRepairCost(data.repairCost())
                    .withEnchantments(data.enchantments());
                if (data.customName() != null) {
                    phases[i] = phases[i]
                        .withCustomName(data.customName().copy());
                }
                if (data.itemName() != null) {
                    phases[i] = phases[i]
                        .withItemName(data.itemName().copy());
                } else {
                    phases[i] = phases[i]
                        .withItemName(original.getDescription().copy().append(i == 0 ? ALPHA_NAME_SUFFIX : BETA_NAME_SUFFIX));
                }
            } else {
                phases[i] = Phase.EMPTY
                    .withCustomName(original.getDescription().copy().append(i == 0 ? ALPHA_NAME_SUFFIX : BETA_NAME_SUFFIX));
            }
        }
        return new Multiphase(phases[0], phases[1]);
    }

    public void applyToStack(ItemStack stack) {
        this.alpha.applyToStack(stack);
    }

    public void cyclePhases(ItemStack stack) {
        Component customName;
        if (this.beta.customName == null) {
            customName = stack.get(DataComponents.CUSTOM_NAME);
            stack.remove(DataComponents.CUSTOM_NAME);
        } else {
            customName = stack.set(DataComponents.CUSTOM_NAME, this.beta.customName);
        }

        Component itemName;
        if (this.beta.itemName == null) {
            itemName = stack.get(DataComponents.ITEM_NAME);
            stack.remove(DataComponents.ITEM_NAME);
        } else {
            itemName = stack.set(DataComponents.ITEM_NAME, this.beta.itemName);
        }

        Integer repairCost = stack.set(DataComponents.REPAIR_COST, this.beta.repairCost);
        if (repairCost == null || repairCost < 0) repairCost = 0;

        ItemEnchantments enchantments = stack.set(DataComponents.ENCHANTMENTS, this.beta.enchantments);
        if (enchantments == null) enchantments = ItemEnchantments.EMPTY;

        Phase newAlpha = this.alpha
            .withCustomName(customName)
            .withItemName(itemName)
            .withRepairCost(repairCost)
            .withEnchantments(enchantments);
        stack.set(ModComponents.MULTIPHASE, new Multiphase(this.beta, newAlpha));
    }

    private static void encode(RegistryFriendlyByteBuf buf, Multiphase multiphase) {
        Phase.STREAM_CODEC.encode(buf, multiphase.alpha);
        Phase.STREAM_CODEC.encode(buf, multiphase.beta);
    }

    private static Multiphase decode(RegistryFriendlyByteBuf buf) {
        return new Multiphase(
            Phase.STREAM_CODEC.decode(buf),
            Phase.STREAM_CODEC.decode(buf)
        );
    }

    public Multiphase copy() {
        return new Multiphase(this.alpha, this.beta);
    }

    public record Phase(
        @Nullable Component customName, @Nullable Component itemName,
        int repairCost, @NotNull ItemEnchantments enchantments
    ) {
        public static final Phase EMPTY = new Phase(
            null, null, 0, ItemEnchantments.EMPTY
        );

        public static final Codec<Phase> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.FLAT_CODEC.lenientOptionalFieldOf("customName")
                .forGetter(phase -> Optional.ofNullable(phase.customName())),
            ComponentSerialization.FLAT_CODEC.lenientOptionalFieldOf("itemName")
                .forGetter(phase -> Optional.ofNullable(phase.itemName())),
            Codec.INT.fieldOf("repairCost").forGetter(Phase::repairCost),
            ItemEnchantments.CODEC.fieldOf("enchantments").forGetter(Phase::enchantments)
        ).apply(inst, (customName, itemName, repairCost, enchantments) ->
            new Phase(customName.orElse(null), itemName.orElse(null), repairCost, enchantments)));
        public static final StreamCodec<RegistryFriendlyByteBuf, Phase> STREAM_CODEC = StreamCodec.of(Phase::encode, Phase::decode);

        public Phase(@Nullable Component name, @Nullable ItemEnchantments enchantments) {
            this(name, 0, enchantments == null ? ItemEnchantments.EMPTY : enchantments);
        }

        public Phase(@Nullable Component name, int repairCost, @Nullable ItemEnchantments enchantments) {
            this(name, name, repairCost, enchantments == null ? ItemEnchantments.EMPTY : enchantments);
        }

        public static Phase make(Component name, @Nullable ItemEnchantments enchantments) {
            return EMPTY
                .withCustomName(name)
                .withEnchantments(enchantments == null ? ItemEnchantments.EMPTY : enchantments);
        }

        public Phase withCustomName(@Nullable Component customName) {
            return new Phase(customName, this.itemName, this.repairCost, this.enchantments);
        }

        public Phase withItemName(@Nullable Component itemName) {
            return new Phase(this.customName, itemName, this.repairCost, this.enchantments);
        }

        public Phase withRepairCost(int repairCost) {
            return new Phase(this.customName, this.itemName, repairCost, this.enchantments);
        }

        public Phase withEnchantments(ItemEnchantments enchantments) {
            return new Phase(this.customName, this.itemName, this.repairCost, enchantments);
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
            return new Phase(this.customName, this.itemName, this.repairCost, originalMut.toImmutable());
        }

        public void applyToStack(ItemStack stack) {
            stack.set(DataComponents.CUSTOM_NAME, this.customName());
            stack.set(DataComponents.ITEM_NAME, this.itemName());
            stack.set(DataComponents.REPAIR_COST, this.repairCost());
            stack.set(DataComponents.ENCHANTMENTS, this.enchantments());
        }

        private static void encode(RegistryFriendlyByteBuf buf, Phase phase) {
            ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buf, Optional.ofNullable(phase.customName));
            ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buf, Optional.ofNullable(phase.itemName));
            ByteBufCodecs.INT.encode(buf, phase.repairCost);
            ItemEnchantments.STREAM_CODEC.encode(buf, phase.enchantments);
        }

        private static Phase decode(RegistryFriendlyByteBuf buf) {
            return new Phase(
                ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buf).orElse(null),
                ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buf).orElse(null),
                ByteBufCodecs.INT.decode(buf),
                ItemEnchantments.STREAM_CODEC.decode(buf)
            );
        }

        @Override
        public String toString() {
            return "Phase{customName: %s, itemName: %s, repairCost: %s, enchantments: %s}"
                .formatted(this.customName, this.itemName, this.repairCost, this.enchantments);
        }
    }

    public record PhaseData(
        @Nullable Component customName, @Nullable Component itemName,
        int repairCost, @Nullable ItemEnchantments enchantments
    ) {
        public static PhaseData of(
            @Nullable Component customName, @Nullable Component itemName,
            int repairCost, @Nullable ItemEnchantments enchantments
        ) {
            return new PhaseData(customName, itemName, repairCost, enchantments);
        }

        @Override
        public ItemEnchantments enchantments() {
            return this.enchantments == null ? ItemEnchantments.EMPTY : this.enchantments;
        }

        public PhaseData withCustomName(@Nullable Component customName) {
            return new PhaseData(customName, itemName, repairCost, enchantments);
        }

        public PhaseData withItemName(@Nullable Component itemName) {
            return new PhaseData(customName, itemName, repairCost, enchantments);
        }

        public PhaseData withRepairCost(int repairCost) {
            return new PhaseData(customName, itemName, repairCost, enchantments);
        }

        public PhaseData withEnchantments(@Nullable ItemEnchantments enchantments) {
            return new PhaseData(customName, itemName, repairCost, enchantments);
        }
    }
}
