package dev.dubhe.anvilcraft.api.recipe.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModCustomDataComponents;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public abstract class MultiphaseData implements ICustomDataComponent<MultiphaseRef> {
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
            case null, default -> throw new IllegalArgumentException("Find invalid type. Expect two, four or eight, get " + type);
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

    private static class Two extends MultiphaseData {
        public static final String TYPE = "two";

        @Override
        protected String type() {
            return Two.TYPE;
        }

        @Override
        public MultiphaseRef make(ResultContext ctx) {
            LinkedList<Multiphase.Phase> phases = new LinkedList<>();
            for (int i = 0; i < 2; i++) {
                ItemStack stack = ctx.getInput(RecipeInputSlot.input(i));
                phases.add(
                    Multiphase.Phase.create(i)
                        .withCustomName(stack.get(DataComponents.CUSTOM_NAME))
                        .withItemName(ctx.getResult().getItem().getDescription().copy().append(Multiphase.makeSuffix(i)))
                        .withRepairCost(stack.getOrDefault(DataComponents.REPAIR_COST, 0))
                        .withEnchantments(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY))
                );
            }
            return new MultiphaseRef(new Multiphase(phases));
        }
    }

    private static class Four extends MultiphaseData {
        public static final String TYPE = "four";

        @Override
        protected String type() {
            return Four.TYPE;
        }

        @Override
        public MultiphaseRef make(ResultContext ctx) {
            LinkedList<Multiphase.Phase> phases = new LinkedList<>();
            for (int i = 0; i < 2; i++) {
                ItemStack stack = ctx.getInput(RecipeInputSlot.input(i * 2));
                ItemStack stack1 = ctx.getInput(RecipeInputSlot.input(i * 2 + 1));
                phases.add(
                    Multiphase.Phase.create(i)
                        .withCustomName(stack.get(DataComponents.CUSTOM_NAME))
                        .withItemName(ctx.getResult().getItem().getDescription().copy().append(Multiphase.makeSuffix(i)))
                        .withRepairCost(stack.getOrDefault(DataComponents.REPAIR_COST, 0))
                        .addRepairCost(stack1.getOrDefault(DataComponents.REPAIR_COST, 0))
                        .withEnchantments(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack1.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack1.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY))
                );
            }
            return new MultiphaseRef(new Multiphase(phases));
        }
    }

    private static class Eight extends MultiphaseData {
        public static final String TYPE = "eight";

        @Override
        protected String type() {
            return Eight.TYPE;
        }

        @Override
        public MultiphaseRef make(ResultContext ctx) {
            LinkedList<Multiphase.Phase> phases = new LinkedList<>();
            for (int i = 0; i < 2; i++) {
                ItemStack stack = ctx.getInput(RecipeInputSlot.input(i * 4));
                ItemStack stack1 = ctx.getInput(RecipeInputSlot.input(i * 4 + 1));
                ItemStack stack2 = ctx.getInput(RecipeInputSlot.input(i * 4 + 2));
                ItemStack stack3 = ctx.getInput(RecipeInputSlot.input(i * 4 + 3));
                phases.add(
                    Multiphase.Phase.create(i)
                        .withCustomName(stack.get(DataComponents.CUSTOM_NAME))
                        .withItemName(ctx.getResult().getItem().getDescription().copy().append(Multiphase.makeSuffix(i)))
                        .withRepairCost(stack.getOrDefault(DataComponents.REPAIR_COST, 0))
                        .addRepairCost(stack1.getOrDefault(DataComponents.REPAIR_COST, 0))
                        .addRepairCost(stack2.getOrDefault(DataComponents.REPAIR_COST, 0))
                        .addRepairCost(stack3.getOrDefault(DataComponents.REPAIR_COST, 0))
                        .withEnchantments(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack1.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack1.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack2.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack2.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack3.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .addEnchantments(stack3.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY))
                );
            }
            return new MultiphaseRef(new Multiphase(phases));
        }
    }

    public static class Type implements ICustomDataComponent.Type<MultiphaseData> {
        public static final MapCodec<MultiphaseData> CODEC = Codec.STRING
            .fieldOf("input_type")
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
