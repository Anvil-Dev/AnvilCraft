package dev.dubhe.anvilcraft.api.recipe.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModCustomDataComponents;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import lombok.EqualsAndHashCode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@EqualsAndHashCode
public abstract class MultiphaseData implements ICustomDataComponent<MultiphaseRef> {
    private final List<RequiredEntry> required;

    private MultiphaseData(List<RequiredEntry> required) {
        this.required = required;
    }

    public static MultiphaseData two() {
        return new Two();
    }

    public static MultiphaseData four() {
        return new Four();
    }

    public static MultiphaseData eight() {
        return new Eight();
    }

    private static MultiphaseData fromType(String type) {
        return switch (type) {
            case Two.TYPE -> new Two();
            case Four.TYPE -> new Four();
            case Eight.TYPE -> new Eight();
            case null, default -> throw new IllegalArgumentException("Find invalid type. Expect two, four and eight, get " + type);
        };
    }

    @Override
    public DataComponentType<MultiphaseRef> getDataComponentType() {
        return ModComponents.MULTIPHASE;
    }

    @Override
    public Type getType() {
        return ModCustomDataComponents.MULTIPHASE.get();
    }

    @Override
    public List<RequiredEntry> getRequired() {
        return this.required;
    }

    @Override
    public MultiphaseRef merge(MultiphaseRef oldData, MultiphaseRef newData) {
        Multiphase old = oldData.toMultiphase();
        if (old == null || old.isEmpty()) return newData;
        oldData.discard(ServerLifecycleHooks.getCurrentServer().registryAccess());
        LinkedList<Multiphase.Phase> oldPhases = old.phases();
        LinkedList<Multiphase.Phase> newPhases = newData.toMultiphase().phases();
        if (oldPhases.isEmpty()) return newData;

        int newFirst = newPhases.peekFirst().index();
        int oldIndex = -1;
        for (Multiphase.Phase phase : oldPhases) {
            if (phase.index() == newFirst) {
                oldIndex = phase.index();
                break;
            }
        }
        if (oldIndex == -1) return newData;

        for (int i = 0, phasesSize = newPhases.size(), oldSize = oldPhases.size(); i < phasesSize; i++) {
            int finalI = i;
            ListUtil.safelyGet(oldPhases, i + oldIndex % oldSize)
                .map(newPhases.get(i)::merge)
                .ifPresent(phase -> newPhases.set(finalI, phase));
        }
        return newData;
    }

    @Override
    public void applyToStack(ItemStack stack, @Nullable MultiphaseRef value) {
        ICustomDataComponent.super.applyToStack(stack, value);
        if (value == null) return;
        value.applyToStack(stack);
    }

    protected abstract String type();

    private static @Nullable Component processCustomName(@Nullable Object customName) {
        return customName instanceof Component it ? it : null;
    }

    private static int processRepairCost(@Nullable Object repairCost) {
        return repairCost instanceof Integer it ? it : 0;
    }

    private static ItemEnchantments processItemEnchantments(@Nullable Object enchantments) {
        return enchantments instanceof ItemEnchantments it ? it : ItemEnchantments.EMPTY;
    }

    private static class Two extends MultiphaseData {
        private static final List<RequiredEntry> REQUIRED = new ArrayList<>();
        public static final String TYPE = "two";

        Two() {
            super(Two.getOrFillRequired());
        }

        @SuppressWarnings("SameReturnValue")
        private static List<RequiredEntry> getOrFillRequired() {
            if (!Two.REQUIRED.isEmpty()) return Two.REQUIRED;
            for (int i = 0; i < 2; i++) {
                Two.REQUIRED.add(new RequiredEntry(i, DataComponents.CUSTOM_NAME, true));
                Two.REQUIRED.add(new RequiredEntry(i, DataComponents.REPAIR_COST, true));
                Two.REQUIRED.add(new RequiredEntry(i, DataComponents.ENCHANTMENTS, true));
            }
            return Two.REQUIRED;
        }

        @Override
        protected String type() {
            return Two.TYPE;
        }

        @Override
        public MultiphaseRef make(List<Object> data) {
            LinkedList<Multiphase.Phase> phases = new LinkedList<>();
            for (int i = 0; i < 2; i++) {
                int base = i * 3;
                phases.add(
                    Multiphase.Phase.create(i)
                        .withCustomName(MultiphaseData.processCustomName(data.get(base)))
                        .withRepairCost(MultiphaseData.processRepairCost(data.get(base + 1)))
                        .withEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 2)))
                );
            }
            return new MultiphaseRef(new Multiphase(phases));
        }
    }

    private static class Four extends MultiphaseData {
        private static final List<RequiredEntry> REQUIRED = new ArrayList<>();
        public static final String TYPE = "four";

        Four() {
            super(Four.getOrFillRequired());
        }

        @SuppressWarnings("SameReturnValue")
        private static List<RequiredEntry> getOrFillRequired() {
            if (!Four.REQUIRED.isEmpty()) return Four.REQUIRED;
            Four.REQUIRED.add(new RequiredEntry(0, DataComponents.CUSTOM_NAME, true));
            Four.REQUIRED.add(new RequiredEntry(2, DataComponents.CUSTOM_NAME, true));
            for (int i = 0; i < 4; i++) {
                Four.REQUIRED.add(new RequiredEntry(i, DataComponents.REPAIR_COST, true));
                Four.REQUIRED.add(new RequiredEntry(i, DataComponents.ENCHANTMENTS, true));
            }
            return Four.REQUIRED;
        }

        @Override
        protected String type() {
            return Four.TYPE;
        }

        @Override
        public MultiphaseRef make(List<Object> data) {
            LinkedList<Multiphase.Phase> phases = new LinkedList<>();
            Component[] customNames = new Component[] {
                MultiphaseData.processCustomName(data.getFirst()),
                MultiphaseData.processCustomName(data.get(1))
            };
            for (int i = 0; i < 2; i++) {
                int base = i * 4 + 2;
                phases.add(
                    Multiphase.Phase.create(i)
                        .withCustomName(customNames[i])
                        .withRepairCost(MultiphaseData.processRepairCost(data.get(base)))
                        .addRepairCost(MultiphaseData.processRepairCost(data.get(base + 2)))
                        .withEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 1)))
                        .addEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 3)))
                );
            }
            return new MultiphaseRef(new Multiphase(phases));
        }
    }

    private static class Eight extends MultiphaseData {
        private static final List<RequiredEntry> REQUIRED = new ArrayList<>();
        public static final String TYPE = "eight";

        Eight() {
            super(Eight.getOrFillRequired());
        }

        @SuppressWarnings("SameReturnValue")
        private static List<RequiredEntry> getOrFillRequired() {
            if (!Eight.REQUIRED.isEmpty()) return Eight.REQUIRED;
            Eight.REQUIRED.add(new RequiredEntry(0, DataComponents.CUSTOM_NAME, true));
            Eight.REQUIRED.add(new RequiredEntry(4, DataComponents.CUSTOM_NAME, true));
            for (int i = 0; i < 8; i++) {
                Eight.REQUIRED.add(new RequiredEntry(i, DataComponents.REPAIR_COST, true));
                Eight.REQUIRED.add(new RequiredEntry(i, DataComponents.ENCHANTMENTS, true));
            }
            return Eight.REQUIRED;
        }

        @Override
        protected String type() {
            return Eight.TYPE;
        }

        @Override
        public MultiphaseRef make(List<Object> data) {
            LinkedList<Multiphase.Phase> phases = new LinkedList<>();
            Component[] customNames = new Component[] {
                data.getFirst() instanceof Component it ? it : null,
                data.get(1) instanceof Component it ? it : null
            };
            for (int i = 0; i < 2; i++) {
                int base = i * 8 + 2;
                phases.add(
                    Multiphase.Phase.create(i)
                        .withCustomName(customNames[i])
                        .withRepairCost(MultiphaseData.processRepairCost(data.get(base)))
                        .addRepairCost(MultiphaseData.processRepairCost(data.get(base + 2)))
                        .addRepairCost(MultiphaseData.processRepairCost(data.get(base + 4)))
                        .addRepairCost(MultiphaseData.processRepairCost(data.get(base + 6)))
                        .withEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 1)))
                        .addEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 3)))
                        .addEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 5)))
                        .addEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 7)))
                );
            }
            return new MultiphaseRef(new Multiphase(phases));
        }
    }

    public static class Type implements ICustomDataComponent.Type<MultiphaseData> {
        public static final MapCodec<MultiphaseData> CODEC = Codec.STRING.fieldOf("input_type")
            .xmap(MultiphaseData::fromType, MultiphaseData::type);
        public static final StreamCodec<RegistryFriendlyByteBuf, MultiphaseData> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(MultiphaseData::fromType, MultiphaseData::type).cast();

        @Override
        public MapCodec<MultiphaseData> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MultiphaseData> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
