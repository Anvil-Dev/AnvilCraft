package dev.dubhe.anvilcraft.integration.emi.stack;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SelectOneEmiStack extends EmiStack {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private final List<EmiIngredient> stacks = new ArrayList<>();

    private SelectOneEmiStack(@NotNull List<EmiIngredient> stacks) {
        for (EmiIngredient stack : stacks) {
            this.stacks.add(stack.copy());
        }
    }

    private SelectOneEmiStack(@NotNull SelectOne selectOne) {
        for (RecipeOutcome outcome : selectOne.getOutcomes()) {
            if (outcome instanceof SpawnItem item) {
                this.stacks.add(EmiStack.of(item.getResult()).setChance((float) outcome.getChance()));
            }
            if (outcome instanceof SetBlock block) {
                this.stacks.add(BlockStateEmiStack.of(block.getResult()).setChance((float) outcome.getChance()));
            }
        }
    }

    public static @NotNull SelectOneEmiStack of(@NotNull SelectOne selectOne) {
        return new SelectOneEmiStack(selectOne);
    }

    @Override
    public SelectOneEmiStack copy() {
        return new SelectOneEmiStack(this.stacks);
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        if (MINECRAFT.level == null || this.stacks.isEmpty()) return;
        int index = (int) Math.floor((MINECRAFT.level.getGameTime() % (this.stacks.size() * 20)) / 20.0);
        this.stacks.get(index).render(draw, x, y, delta, flags);
    }

    @Override
    public boolean isEmpty() {
        return this.stacks.isEmpty();
    }

    @Override
    public CompoundTag getNbt() {
        return new CompoundTag();
    }

    @Override
    public Object getKey() {
        return "SelectOne$" + this.stacks.hashCode();
    }

    @Override
    public ResourceLocation getId() {
        return AnvilCraft.of("select_one_" + this.stacks.hashCode());
    }

    @Override
    public List<Component> getTooltipText() {
        return List.of();
    }

    @Override
    public Component getName() {
        return Component.literal("SelectOne");
    }
}
