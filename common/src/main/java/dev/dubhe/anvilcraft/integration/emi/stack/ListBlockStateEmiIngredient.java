package dev.dubhe.anvilcraft.integration.emi.stack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListBlockStateEmiIngredient implements EmiIngredient {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private final List<BlockStateEmiStack> blocks;
    private long amount;
    private float chance;

    private ListBlockStateEmiIngredient(List<BlockStateEmiStack> blocks, long amount, float chance) {
        this.blocks = blocks;
        this.amount = amount;
        this.chance = chance;
    }

    public static @NotNull ListBlockStateEmiIngredient of(
            List<BlockStateEmiStack> blocks, long amount, float chance) {
        return new ListBlockStateEmiIngredient(blocks, amount, chance);
    }

    public static @NotNull ListBlockStateEmiIngredient of(
            List<BlockStateEmiStack> blocks, long amount) {
        return ListBlockStateEmiIngredient.of(blocks, amount, 1.0f);
    }

    public static @NotNull ListBlockStateEmiIngredient of(List<BlockStateEmiStack> blocks) {
        return ListBlockStateEmiIngredient.of(blocks, 1);
    }

    @Override
    public List<EmiStack> getEmiStacks() {
        return this.blocks.stream().map(b -> (EmiStack) b).toList();
    }

    @Override
    public ListBlockStateEmiIngredient copy() {
        return ListBlockStateEmiIngredient.of(this.blocks, this.amount, this.chance);
    }

    @Override
    public long getAmount() {
        return this.amount;
    }

    @Override
    public ListBlockStateEmiIngredient setAmount(long amount) {
        this.amount = amount;
        return this;
    }

    @Override
    public float getChance() {
        return this.chance;
    }

    @Override
    public ListBlockStateEmiIngredient setChance(float chance) {
        this.chance = chance;
        return this;
    }

    public BlockStateEmiStack get() {
        if (MINECRAFT.level == null || this.blocks.isEmpty()) return BlockStateEmiStack.EMPTY;
        return this.blocks.get(
                (int) Math.floor((MINECRAFT.level.getGameTime() % (this.blocks.size() * 20)) / 20.0));
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        this.get().render(draw, x, y, delta, flags);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        return this.get().getTooltip();
    }
}
