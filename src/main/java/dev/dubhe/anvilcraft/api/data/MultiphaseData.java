package dev.dubhe.anvilcraft.api.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModCustomDataComponents;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import dev.dubhe.anvilcraft.util.ListUtil;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public abstract class MultiphaseData implements ICustomDataComponent<MultiphaseRef> {
    private final Object2BooleanMap<Pair<Integer, DataComponentType<?>>> required;

    private MultiphaseData(Object2BooleanMap<Pair<Integer, DataComponentType<?>>> required) {
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
    public Object2BooleanMap<Pair<Integer, DataComponentType<?>>> getRequiredOthers() {
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

    private static @NotNull ItemEnchantments processItemEnchantments(@Nullable Object enchantments) {
        return enchantments instanceof ItemEnchantments it ? it : ItemEnchantments.EMPTY;
    }

    private static class Two extends MultiphaseData {
        private static final Object2BooleanMap<Pair<Integer, DataComponentType<?>>> REQUIRED = new Object2BooleanArrayMap<>();
        public static final String TYPE = "two";

        Two() {
            super(Two.getOrFillRequired());
        }

        @SuppressWarnings("SameReturnValue")
        private static Object2BooleanMap<Pair<Integer, DataComponentType<?>>> getOrFillRequired() {
            if (!Two.REQUIRED.isEmpty()) return Two.REQUIRED;
            for (int i = 0; i < 2; i++) {
                Two.REQUIRED.put(new Pair<>(i, DataComponents.CUSTOM_NAME), true);
                Two.REQUIRED.put(new Pair<>(i, DataComponents.REPAIR_COST), true);
                Two.REQUIRED.put(new Pair<>(i, DataComponents.ENCHANTMENTS), true);
                Two.REQUIRED.put(new Pair<>(i, ModComponents.MERCILESS_ENCHANTMENTS), true);
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
                int base = i * 4;
                phases.add(
                    Multiphase.Phase.create(i)
                        .withCustomName(MultiphaseData.processCustomName(data.get(base)))
                        .withRepairCost(MultiphaseData.processRepairCost(data.get(base + 1)))
                        .withEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 2)))
                        .withStoredEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 3)))
                );
            }
            return new MultiphaseRef(new Multiphase(phases));
        }
    }

    private static class Four extends MultiphaseData {
        private static final Object2BooleanMap<Pair<Integer, DataComponentType<?>>> REQUIRED = new Object2BooleanArrayMap<>();
        public static final String TYPE = "four";

        Four() {
            super(Four.getOrFillRequired());
        }

        @SuppressWarnings("SameReturnValue")
        private static Object2BooleanMap<Pair<Integer, DataComponentType<?>>> getOrFillRequired() {
            if (!Four.REQUIRED.isEmpty()) return Four.REQUIRED;
            Four.REQUIRED.put(new Pair<>(0, DataComponents.CUSTOM_NAME), true);
            Four.REQUIRED.put(new Pair<>(2, DataComponents.CUSTOM_NAME), true);
            for (int i = 0; i < 4; i++) {
                Four.REQUIRED.put(new Pair<>(i, DataComponents.REPAIR_COST), true);
                Four.REQUIRED.put(new Pair<>(i, DataComponents.ENCHANTMENTS), true);
                Four.REQUIRED.put(new Pair<>(i, ModComponents.MERCILESS_ENCHANTMENTS), true);
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
                int base = i * 6 + 2;
                phases.add(
                    Multiphase.Phase.create(i)
                        .withCustomName(customNames[(int) Math.floor(i / 2.0)])
                        .withRepairCost(MultiphaseData.processRepairCost(data.get(base)))
                        .addRepairCost(MultiphaseData.processRepairCost(data.get(base + 3)))
                        .withEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 1)))
                        .addEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 4)))
                        .withStoredEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 2)))
                        .addStoredEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 5)))
                );
            }
            return new MultiphaseRef(new Multiphase(phases));
        }
    }

    private static class Eight extends MultiphaseData {
        private static final Object2BooleanMap<Pair<Integer, DataComponentType<?>>> REQUIRED = new Object2BooleanArrayMap<>();
        public static final String TYPE = "eight";

        Eight() {
            super(Eight.getOrFillRequired());
        }

        @SuppressWarnings("SameReturnValue")
        private static Object2BooleanMap<Pair<Integer, DataComponentType<?>>> getOrFillRequired() {
            if (!Eight.REQUIRED.isEmpty()) return Eight.REQUIRED;
            Eight.REQUIRED.put(new Pair<>(0, DataComponents.CUSTOM_NAME), true);
            Eight.REQUIRED.put(new Pair<>(4, DataComponents.CUSTOM_NAME), true);
            for (int i = 0; i < 8; i++) {
                Eight.REQUIRED.put(new Pair<>(i, DataComponents.REPAIR_COST), true);
                Eight.REQUIRED.put(new Pair<>(i, DataComponents.ENCHANTMENTS), true);
                Eight.REQUIRED.put(new Pair<>(i, ModComponents.MERCILESS_ENCHANTMENTS), true);
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
                int base = i * 12 + 2;
                phases.add(
                    Multiphase.Phase.create(i)
                        .withCustomName(customNames[(int) Math.floor(i / 4.0)])
                        .withRepairCost(MultiphaseData.processRepairCost(data.get(base)))
                        .addRepairCost(MultiphaseData.processRepairCost(data.get(base + 3)))
                        .addRepairCost(MultiphaseData.processRepairCost(data.get(base + 6)))
                        .addRepairCost(MultiphaseData.processRepairCost(data.get(base + 9)))
                        .withEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 1)))
                        .addEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 4)))
                        .addEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 7)))
                        .addEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 10)))
                        .withStoredEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 2)))
                        .addStoredEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 5)))
                        .addStoredEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 8)))
                        .addStoredEnchantments(MultiphaseData.processItemEnchantments(data.get(base + 11)))
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
        public @NotNull MapCodec<MultiphaseData> codec() {
            return Type.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MultiphaseData> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
